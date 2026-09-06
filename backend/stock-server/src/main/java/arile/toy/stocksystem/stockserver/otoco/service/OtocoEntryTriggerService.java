package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.OtocoLockRegistry;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoEntryTriggerService {

    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoLockRegistry otocoLockRegistry;
    private final OtocoRepository otocoRepository;
    private final OrderService orderService;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final AccountApiClient accountApiClient;

    public void getExternalTickMessageAndTriggerEntry(TradePriceTickMessage tick) {
        ReentrantLock lock = otocoLockRegistry.lock(tick.stockCode());

        lock.lock();
        try {
            trackWithinLock(tick);
        } finally {
            lock.unlock();
        }
    }

    private void trackWithinLock(TradePriceTickMessage tick) {

        String stockCode = tick.stockCode();
        int currentPrice = tick.curPrice();

        List<OtocoDto> snapshot = new ArrayList<>(otocoEntryBookRegistry.getAll(stockCode));

        for (OtocoDto dto : snapshot) {

            boolean isTriggered = dto.entryDirection() == OtocoEntryDirection.ABOVE
                    ? currentPrice >= dto.entryTriggerPrice()
                    : currentPrice <= dto.entryTriggerPrice();

            if (!isTriggered) {
                continue;
            }

            otocoEntryBookRegistry.remove(stockCode, dto.otocoId());
            triggerEntryAndRegisterOrder(dto);
        }
    }

    @Transactional
    public void triggerEntryAndRegisterOrder(OtocoDto dto) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(dto.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (entity.getOtocoStatus() != OtocoStatus.WAITING_ENTRY) {
            return; // 경합 방지 (이미 취소되었거나 처리된 경우)
        }

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromOtocoEntry(dto);

        OrderEntity savedOrder;
        try {
            savedOrder = orderService.registerOrder(event, true);
        } catch (Exception e) {
            log.error("Otoco entry -> order registration failed. otocoId={}, username={}, stockCode={}",
                    dto.otocoId(), dto.username(), dto.stockCode(), e);
            compensateFailedEntry(entity);
            return;
        }

        if (savedOrder == null) {
            compensateFailedEntry(entity);
            return;
        }

        entity.setEntryOrderId(savedOrder.getOrderId());
        entity.changeStatus(OtocoStatus.ENTRY_ORDER_PLACED);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.update(entity.getUsername(), entity.getOtocoId(),
                StockServerOtocoResponseMessage.fromEntity(entity));

        otocoResponseEventPublisher.publishEntryTriggered(OtocoDto.fromEntity(entity));
    }

    private void compensateFailedEntry(OtocoEntity entity) {

        long orderAmount = (long) entity.getEntryTriggerPrice() * entity.getOrderQuantity();
        long refundAmount = entity.getLeverageRatio().isSpot()
                ? orderAmount
                : entity.getLeverageRatio().calculateMarginDeposit(orderAmount);

        boolean refunded = accountApiClient.refundReservedCash(entity.getUsername(), refundAmount);

        if (!refunded) {
            log.error("CRITICAL: Otoco entry trigger compensation refund FAILED. " +
                            "Manual intervention required. otocoId={}, username={}",
                    entity.getOtocoId(), entity.getUsername());
        }

        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        otocoResponseEventPublisher.publishEntryFailed(OtocoDto.fromEntity(entity), OtocoResultCode.ENTRY_FAILED);
    }
}
