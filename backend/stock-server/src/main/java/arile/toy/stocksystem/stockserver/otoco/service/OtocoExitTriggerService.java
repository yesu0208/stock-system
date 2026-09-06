package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.OtocoLockRegistry;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
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
public class OtocoExitTriggerService {

    private final OtocoExitBookRegistry otocoExitBookRegistry;
    private final OtocoLockRegistry otocoLockRegistry;
    private final OtocoRepository otocoRepository;
    private final OrderService orderService;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final AccountApiClient accountApiClient;

    public void getExternalTickMessageAndSettleExit(TradePriceTickMessage tick) {
        ReentrantLock lock = otocoLockRegistry.lock(tick.stockCode());

        lock.lock();
        try {
            settleWithinLock(tick);
        } finally {
            lock.unlock();
        }
    }

    private void settleWithinLock(TradePriceTickMessage tick) {

        String stockCode = tick.stockCode();
        int currentPrice = tick.curPrice();

        List<OtocoDto> snapshot = new ArrayList<>(otocoExitBookRegistry.getAll(stockCode));

        for (OtocoDto dto : snapshot) {

            boolean slHit = currentPrice <= dto.slTriggerPrice();
            boolean tpHit = currentPrice >= dto.tpTriggerPrice();

            if (!slHit && !tpHit) {
                continue;
            }

            // 갭 상승/하락으로 한 틱에 두 조건이 동시에 만족되는 경우, 자본 보호를 우선해 SL(Stop Loss)을 먼저 체크
            OtocoLeg leg = slHit ? OtocoLeg.STOP_LOSS : OtocoLeg.TAKE_PROFIT;

            otocoExitBookRegistry.remove(stockCode, dto.otocoId());
            triggerExit(dto, leg);
        }
    }

    @Transactional
    public void triggerExit(OtocoDto dto, OtocoLeg leg) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(dto.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (entity.getOtocoStatus() != OtocoStatus.WAITING_EXIT) {
            return; // 경합 방지 (이미 취소되었거나 처리된 경우)
        }

        Integer exitPrice = leg == OtocoLeg.TAKE_PROFIT ? entity.getTpTriggerPrice() : entity.getSlTriggerPrice();

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromOtocoExit(dto, exitPrice);

        try {
            // 재고(주식)는 진입 체결 시 1회만 예약했으므로 fromAutoOrder=true로 재예약을 건너뜀
            orderService.registerOrder(event, true);
        } catch (Exception e) {
            log.error("Otoco exit -> order registration failed. otocoId={}, username={}, leg={}",
                    dto.otocoId(), dto.username(), leg, e);
            compensateFailedExit(entity, leg);
            return;
        }

        entity.markCompleted(leg);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());

        otocoResponseEventPublisher.publishExitTriggered(OtocoDto.fromEntity(entity), leg);
    }

    private void compensateFailedExit(OtocoEntity entity, OtocoLeg leg) {

        boolean refunded = entity.getLeverageRatio().isSpot()
                ? accountApiClient.refundReservedStock(entity.getUsername(), entity.getStockCode(), entity.getOrderQuantity())
                : accountApiClient.refundReservedLeverageStock(entity.getUsername(), entity.getStockCode(),
                entity.getLeverageRatio().name(), entity.getOrderQuantity());

        if (!refunded) {
            log.error("CRITICAL: Otoco exit trigger compensation refund FAILED. " +
                            "Manual intervention required. otocoId={}, username={}, leg={}",
                    entity.getOtocoId(), entity.getUsername(), leg);
        }

        // 보유 포지션 자체는 그대로 남고, OCO 추적만 종료
        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        otocoResponseEventPublisher.publishExitFailed(OtocoDto.fromEntity(entity), OtocoResultCode.INTERNAL_ERROR);
    }
}
