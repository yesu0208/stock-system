package arile.toy.stocksystem.stockserver.autoorder.service;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutoOrderService {

    private final AutoOrderRepository autoOrderRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    public void registerAutoOrder(StockServerAutoOrderRequestEvent request) {

        long orderAmount = (long) request.orderPrice() * request.orderQuantity();
        LeverageRatio leverageRatio = request.leverageRatio() == null ? LeverageRatio.SPOT : request.leverageRatio();
        long reserveAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, orderAmount);

        if (request.autoOrderType() == AutoOrderType.BUY) {
            boolean reserved = accountApiClient
                    .reserveCash(request.username(), reserveAmount);

            if (!reserved) {
                autoOrderResponseEventPublisher.publishError(request, AutoOrderResultCode.INSUFFICIENT_BALANCE);
                return;
            }
        } else {
            boolean reserved = leverageRatio.isSpot()
                    ? accountApiClient.reserveStock(request.username(), request.stockCode(), request.orderQuantity())
                    : accountApiClient.reserveLeverageStock(request.username(), request.stockCode(), leverageRatio.name(), request.orderQuantity());

            if (!reserved) {
                autoOrderResponseEventPublisher.publishError(request, AutoOrderResultCode.INSUFFICIENT_STOCK);
                return;
            }
        }

        AutoOrderEntity savedAutoOrder;

        try {
            AutoOrderEntity autoOrderEntity = AutoOrderEntity.of(
                    request.username(),
                    request.stockCode(),
                    request.autoOrderType(),
                    leverageRatio,
                    request.triggerPrice(),
                    request.orderPrice(),
                    request.orderQuantity(),
                    AutoOrderStatus.ACTIVE
            );
            savedAutoOrder = autoOrderRepository.save(autoOrderEntity);

            var autoOrderDto = AutoOrderDto.fromEntity(savedAutoOrder);
            autoOrderQueueRegistry.autoOrderEnqueue(autoOrderDto);

        } catch (Exception e) {
            if (request.autoOrderType() == AutoOrderType.BUY) {
                accountApiClient.refundReservedCash(request.username(), reserveAmount);
            } else {
                if (leverageRatio.isSpot()) {
                    accountApiClient.refundReservedStock(
                            request.username(), request.stockCode(), request.orderQuantity());
                } else {
                    accountApiClient.refundReservedLeverageStock(
                            request.username(), request.stockCode(), leverageRatio.name(), request.orderQuantity());
                }
            }
            autoOrderResponseEventPublisher.publishError(request, AutoOrderResultCode.INTERNAL_ERROR);
            throw e;
        }

        var autoOrderResponseMessage = new StockServerAutoOrderResponseMessage(savedAutoOrder.getAutoOrderId(),
                savedAutoOrder.getUsername(), savedAutoOrder.getStockCode(),
                savedAutoOrder.getAutoOrderType(), savedAutoOrder.getLeverageRatio(), savedAutoOrder.getTriggerPrice(),
                savedAutoOrder.getOrderPrice(), savedAutoOrder.getOrderQuantity(),
                savedAutoOrder.getOrderTime());

        stockServerAutoOrderResponseRepository.save(autoOrderResponseMessage);
        autoOrderResponseEventPublisher.publish(autoOrderResponseMessage);
    }

    /** 시스템 강제 취소(장 마감 정리 등)용. 소유자 검증 없이 취소 상태로 변경한다. */
    @Transactional
    public UpdateAutoOrderStatusResult updateAutoOrderStatusByCancel(Long autoOrderId) {

        AutoOrderEntity autoOrderEntity = autoOrderRepository.findByIdForUpdate(autoOrderId)
                .orElseThrow(() -> new IllegalArgumentException("auto order not found"));

        return markCanceled(autoOrderEntity);
    }

    /**
     * 사용자 취소 요청용.
     * 요청자가 자동 주문 소유자이고 요청 종목코드가 자동 주문 종목코드와 같을 때만 취소 상태로 변경.
     * 불일치 시 상태를 변경하지 않고 empty를 반환. (타인 자동 주문 취소, 다른 샤드로의 취소 라우팅 방지)
     */
    @Transactional
    public Optional<UpdateAutoOrderStatusResult> updateAutoOrderStatusByUserCancel(
            Long autoOrderId, String username, String stockCode) {

        AutoOrderEntity autoOrderEntity = autoOrderRepository.findByIdForUpdate(autoOrderId)
                .orElseThrow(() -> new IllegalArgumentException("auto order not found"));

        if (username == null
                || !username.equals(autoOrderEntity.getUsername())
                || !autoOrderEntity.getStockCode().equals(stockCode)) {
            return Optional.empty();
        }

        return Optional.of(markCanceled(autoOrderEntity));
    }

    private UpdateAutoOrderStatusResult markCanceled(AutoOrderEntity autoOrderEntity) {

        AutoOrderStatus prevStatus = autoOrderEntity.getAutoOrderStatus();

        if (prevStatus == AutoOrderStatus.CANCELED ||
                prevStatus == AutoOrderStatus.TRIGGERED) {
            return UpdateAutoOrderStatusResult.of(autoOrderEntity, prevStatus);
        }

        autoOrderEntity.changeAutoOrderStatus(AutoOrderStatus.CANCELED);
        return UpdateAutoOrderStatusResult.of(autoOrderEntity, prevStatus);
    }

    @Transactional
    public UpdateAutoOrderStatusResult updateAutoOrderStatusByTrigger(Long autoOrderId) {

        AutoOrderEntity autoOrderEntity = autoOrderRepository.findByIdForUpdate(autoOrderId)
                .orElseThrow(() -> new IllegalArgumentException("auto order not found"));

        AutoOrderStatus prevStatus = autoOrderEntity.getAutoOrderStatus();

        if (prevStatus != AutoOrderStatus.ACTIVE) {
            return UpdateAutoOrderStatusResult.of(autoOrderEntity, prevStatus);
        }

        autoOrderEntity.changeAutoOrderStatus(AutoOrderStatus.TRIGGERED);
        return UpdateAutoOrderStatusResult.of(autoOrderEntity, prevStatus);
    }

    @Transactional
    public List<AutoOrderEntity> findAllUntriggeredAutoOrders(List<String> stockCodes) {
        return autoOrderRepository.findAllUntriggered(stockCodes);
    }
}
