package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.TrailingStopLockRegistry;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
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
    private final ReserveAmountCalculator reserveAmountCalculator;
    private final TrailingStopTrailPersister trailingStopTrailPersister;

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

                // 재시작 시 추적 상태 복구를 위해 저장 대상으로 표시 (DB 저장은 별도 주기로 수행)
                trailingStopTrailPersister.markDirty(updated);

                // 화면 표시용 부가 작업: 실패해도 같은 틱의 나머지 트레일링 스탑 처리를 멈추지 않음
                try {
                    stockServerTrailingStopResponseRepository.update(
                            updated.username(), updated.trailingStopId(),
                            StockServerTrailingStopResponseMessage.fromDto(updated));
                } catch (Exception e) {
                    log.warn("Trailing stop response update failed. trailingStopId={}", updated.trailingStopId(), e);
                }

                trailingStopResponseEventPublisher.publishTrailingUpdate(updated);
            }
        }
    }

    private void triggerAndRegisterOrder(TrailingStopDto dto) {

        UpdateTrailingStopStatusResult result;
        try {
            result = trailingStopService.updateTrailingStopStatusByTrigger(dto.trailingStopId());
        } catch (Exception e) {
            // 발동 상태 변경 실패(롤백): 북에서 이미 제거했으므로 다시 등록해 다음 틱에 재시도
            log.error("Trailing stop trigger status update failed. trailingStopId={}", dto.trailingStopId(), e);
            trailingStopBookRegistry.register(dto);
            return;
        }

        if (result.previousStatus() != TrailingStopStatus.ACTIVE) {
            return;
        }

        // BUY: 등록 시 initialTriggerPrice 기준으로 예약해 둔 금액과, 실제 발동가(dto.triggerPrice()) 기준
        // 주문 금액의 차액을 먼저 환불해 reservedCash를 "이번에 생성할 주문의 정확한 예약액"으로 맞춤.
        // (트레일링 특성상 발동 시점의 triggerPrice는 initialTriggerPrice보다 항상 작거나 같음.)
        if (dto.trailingStopType() == TrailingStopType.BUY) {
            long reservedAmount = reserveAmountCalculator.calculateReserveAmount(
                    dto.leverageRatio(), (long) dto.initialTriggerPrice() * dto.orderQuantity());
            long orderAmount = reserveAmountCalculator.calculateReserveAmount(
                    dto.leverageRatio(), (long) dto.triggerPrice() * dto.orderQuantity());
            long refund = reservedAmount - orderAmount;

            if (refund > 0 && !accountApiClient.refundReservedCash(dto.username(), refund)) {
                // 차액이 예약된 채로 남음 (장 마감 정산에서 해제됨)
                log.error("Trailing stop trigger difference refund failed. trailingStopId={}, refund={}",
                        dto.trailingStopId(), refund);
            }
        }

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromTrailingStopDto(dto);

        try {
            orderService.registerOrder(event, true);
        } catch (Exception e) {

            log.error("Trailing stop trigger -> order registration failed. trailingStopId={}, username={}, stockCode={}",
                    dto.trailingStopId(), dto.username(), dto.stockCode(), e);

            compensateFailedTrigger(dto);
            return;
        }

        // 주문 등록 성공: 예약금은 주문으로 넘어갔으므로, 이후 부가 작업이 실패해도 보상(환불)하면 안 됨
        try {
            stockServerTrailingStopResponseRepository.delete(dto.username(), dto.trailingStopId());
        } catch (Exception e) {
            log.warn("Trailing stop response delete failed after trigger. trailingStopId={}", dto.trailingStopId(), e);
        }
        trailingStopResponseEventPublisher.publishTrigger(dto.username());
    }

    private void compensateFailedTrigger(TrailingStopDto dto) {

        boolean refunded;

        if (dto.trailingStopType() == TrailingStopType.BUY) {
            long orderAmount = reserveAmountCalculator.calculateReserveAmount(
                    dto.leverageRatio(), (long) dto.triggerPrice() * dto.orderQuantity());
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

        try {
            stockServerTrailingStopResponseRepository.delete(dto.username(), dto.trailingStopId());
        } catch (Exception e) {
            log.warn("Trailing stop response delete failed after trigger compensation. trailingStopId={}",
                    dto.trailingStopId(), e);
        }

        trailingStopResponseEventPublisher.publishTriggerFailure(dto, TrailingStopResultCode.INTERNAL_ERROR);
    }
}
