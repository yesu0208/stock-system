package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.TrailingStopLockRegistry;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.trailingstop.dto.*;
import arile.toy.stocksystem.stockserver.trailingstop.event.publisher.TrailingStopResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import arile.toy.stocksystem.stockserver.trailingstop.repository.StockServerTrailingStopResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrailingStopTriggerService {

    private final TrailingStopBookRegistry trailingStopBookRegistry;
    private final TrailingStopLockRegistry trailingStopLockRegistry;
    private final TrailingStopService trailingStopService;
    private final StockServerTrailingStopResponseRepository stockServerTrailingStopResponseRepository;
    private final OrderService orderService;
    private final TrailingStopResponseEventPublisher trailingStopResponseEventPublisher;
    private final AccountApiClient accountApiClient;

    public void getExternalTickMessageAndTrail(TradePriceTickMessage tick) {
        ReentrantLock lock = trailingStopLockRegistry.lock(tick.stockCode());

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

        // 순회 중 registry.remove/put이 발생할 수 있으므로 스냅샷을 떠서 순회한다.
        List<TrailingStopDto> snapshot = new ArrayList<>(trailingStopBookRegistry.getAll(stockCode));

        for (TrailingStopDto dto : snapshot) {

            boolean isTriggered = dto.trailingStopType() == TrailingStopType.BUY
                    ? currentPrice >= dto.triggerPrice()
                    : currentPrice <= dto.triggerPrice();

            if (isTriggered) {
                trailingStopBookRegistry.remove(stockCode, dto.trailingStopId());
                triggerAndRegisterOrder(dto);
                continue;
            }

            boolean isNewExtreme = dto.trailingStopType() == TrailingStopType.BUY
                    ? currentPrice < dto.basePrice()
                    : currentPrice > dto.basePrice();

            if (isNewExtreme) {
                int newTrigger = TrailingStopPriceCalculator.calcTrigger(dto.trailingStopType(), currentPrice, dto.stopPercent());
                TrailingStopDto updated = dto.withUpdatedTrail(currentPrice, newTrigger);

                trailingStopBookRegistry.update(updated);

                stockServerTrailingStopResponseRepository.update(
                        updated.username(), updated.trailingStopId(),
                        arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage.fromDto(updated));

                trailingStopResponseEventPublisher.publishTrailingUpdate(updated);
            }
        }
    }

    private void triggerAndRegisterOrder(TrailingStopDto dto) {

        var result = trailingStopService.updateTrailingStopStatusByTrigger(dto.trailingStopId());

        if (result.previousStatus() != TrailingStopStatus.ACTIVE) {
            return;
        }

        // BUY: 등록 시 initialTriggerPrice 기준으로 예약해 둔 금액과, 실제 발동가(dto.triggerPrice()) 기준
        // 주문 금액의 차액을 먼저 환불해 reservedCash를 "이번에 생성할 주문의 정확한 예약액"으로 맞춤.
        // (트레일링 특성상 발동 시점의 triggerPrice는 initialTriggerPrice보다 항상 작거나 같음.)
        if (dto.trailingStopType() == TrailingStopType.BUY) {
            long reservedAmount = resolveOrderAmount(dto, dto.initialTriggerPrice());
            long orderAmount = resolveOrderAmount(dto, dto.triggerPrice());
            long refund = reservedAmount - orderAmount;

            if (refund > 0) {
                accountApiClient.refundReservedCash(dto.username(), refund);
            }
        }

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromTrailingStopDto(dto);

        try {
            orderService.registerOrder(event, true);

            stockServerTrailingStopResponseRepository.delete(dto.username(), dto.trailingStopId());
            trailingStopResponseEventPublisher.publishTrigger(dto.username());

        } catch (Exception e) {

            log.error("Trailing stop trigger -> order registration failed. trailingStopId={}, username={}, stockCode={}",
                    dto.trailingStopId(), dto.username(), dto.stockCode(), e);

            compensateFailedTrigger(dto);
        }
    }

    private void compensateFailedTrigger(TrailingStopDto dto) {

        boolean refunded;

        if (dto.trailingStopType() == TrailingStopType.BUY) {
            long orderAmount = resolveOrderAmount(dto, dto.triggerPrice());
            refunded = accountApiClient.refundReservedCash(dto.username(), orderAmount);
        } else {
            refunded = dto.leverageRatio().isSpot()
                    ? accountApiClient.refundReservedStock(dto.username(), dto.stockCode(), dto.orderQuantity())
                    : accountApiClient.refundReservedLeverageStock(dto.username(), dto.stockCode(),
                    dto.leverageRatio().name(), dto.orderQuantity());
        }

        if (!refunded) {
            log.error("CRITICAL: Trailing stop trigger compensation refund FAILED. " +
                            "Manual intervention required. trailingStopId={}, username={}, stockCode={}, type={}",
                    dto.trailingStopId(), dto.username(), dto.stockCode(), dto.trailingStopType());
        }

        stockServerTrailingStopResponseRepository.delete(dto.username(), dto.trailingStopId());

        trailingStopResponseEventPublisher.publishTriggerFailure(dto, TrailingStopResultCode.INTERNAL_ERROR);
    }

    private long resolveOrderAmount(TrailingStopDto dto, Integer price) {
        long rawAmount = (long) price * dto.orderQuantity();
        return dto.leverageRatio().isSpot() ? rawAmount : dto.leverageRatio().calculateMarginDeposit(rawAmount);
    }
}
