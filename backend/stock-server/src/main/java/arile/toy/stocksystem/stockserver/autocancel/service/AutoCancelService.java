package arile.toy.stocksystem.stockserver.autocancel.service;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.autocancel.entity.AutoCancelEntity;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.publisher.AutoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autocancel.repository.AutoCancelRepository;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCancelService {

    private final AutoOrderService autoOrderService;
    private final AutoCancelRepository autoCancelRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoCancelResponseEventPublisher autoCancelResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void registerAutoCancel(AutoCancelRequestEvent request) {

        // 요청자가 자동 주문 소유자가 아니거나 종목코드가 다르면 취소하지 않음 (타인 자동 주문 취소 방지)
        // 취소 응답은 자동 주문 소유자 채널로 발행되므로, 거부 시에는 응답을 보내지 않고 로그만 남김
        var ownedResult = autoOrderService.updateAutoOrderStatusByUserCancel(
                request.autoOrderId(), request.username(), request.stockCode());

        if (ownedResult.isEmpty()) {
            log.warn("Auto cancel rejected: not the auto order owner or stock code mismatch. autoOrderId={}, requester={}, stockCode={}",
                    request.autoOrderId(), request.username(), request.stockCode());
            return;
        }

        UpdateAutoOrderStatusResult result = ownedResult.get();
        var autoOrderEntity = result.autoOrderEntity();

        switch (result.previousStatus()) {
            case CANCELED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.ALREADY_CANCELLED));

            case TRIGGERED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity,false, AutoCancelErrorCode.ALREADY_TRIGGERED));

            default -> {

                try {
                    cancelInternal(autoOrderEntity);
                    publishSuccess(autoOrderEntity);

                } catch (Exception e) {

                    log.error("Auto cancel failed. autoOrderId={}",
                            autoOrderEntity.getAutoOrderId(), e);

                    autoCancelResponseEventPublisher.publish(
                            AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.INTERNAL_ERROR));
                }
            }
        }
    }

    @Transactional
    public void forceAutoCancel(Long autoOrderId) {

        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(autoOrderId);

        if (!result.previousStatus().isOpen()) {
            return;
        }

        var autoOrderEntity = result.autoOrderEntity();

        try {
            cancelInternal(autoOrderEntity);
            publishSuccess(autoOrderEntity);
        } catch (Exception e) {
            log.error("Force auto cancel failed. autoOrderId={}", autoOrderId, e);
        }
    }

    private void cancelInternal(AutoOrderEntity autoOrderEntity) {

        boolean refunded;
        LeverageRatio leverageRatio = autoOrderEntity.getLeverageRatio();

        if (autoOrderEntity.getAutoOrderType() == AutoOrderType.BUY) {

            long orderAmount = (long) autoOrderEntity.getOrderPrice() * autoOrderEntity.getOrderQuantity();
            long refundAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, orderAmount);

            refunded = accountApiClient.refundReservedCash(autoOrderEntity.getUsername(), refundAmount);

            if (!refunded) {
                log.error("Redis cash refund failed. autoOrderId={}, username={}",
                        autoOrderEntity.getAutoOrderId(),
                        autoOrderEntity.getUsername());

                throw new IllegalStateException("Cash refund failed");
            }

        } else {

            refunded = leverageRatio.isSpot()
                    ? accountApiClient.refundReservedStock(autoOrderEntity.getUsername(), autoOrderEntity.getStockCode(), autoOrderEntity.getOrderQuantity())
                    : accountApiClient.refundReservedLeverageStock(autoOrderEntity.getUsername(), autoOrderEntity.getStockCode(),
                    leverageRatio.name(), autoOrderEntity.getOrderQuantity());

            if (!refunded) {
                log.error("Redis stock refund failed. autoOrderId={}, username={}",
                        autoOrderEntity.getAutoOrderId(),
                        autoOrderEntity.getUsername());

                throw new IllegalStateException("Stock refund failed");
            }
        }

        autoCancelRepository.save(
                AutoCancelEntity.of(autoOrderEntity.getAutoOrderId())
        );

        autoOrderQueueRegistry.autoOrderCancel(
                autoOrderEntity.getAutoOrderId(),
                autoOrderEntity.getStockCode()
        );
    }

    private void publishSuccess(AutoOrderEntity autoOrderEntity) {

        AutoCancelResponseEvent event = AutoCancelResponseEvent.of(autoOrderEntity, true, null);

        stockServerAutoOrderResponseRepository.delete(event.username(), event.autoOrderId());

        autoCancelResponseEventPublisher.publish(event);
    }
}
