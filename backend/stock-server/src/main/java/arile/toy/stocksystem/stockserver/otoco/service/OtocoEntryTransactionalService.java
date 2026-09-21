package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoEntryTransactionalService {

    private final OtocoRepository otocoRepository;
    private final OrderService orderService;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void triggerEntryAndRegisterOrder(OtocoDto dto) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(dto.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (entity.getOtocoStatus() != OtocoStatus.WAITING_ENTRY) {
            return;
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
        long refundAmount = reserveAmountCalculator.calculateReserveAmount(entity.getLeverageRatio(), orderAmount);

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
