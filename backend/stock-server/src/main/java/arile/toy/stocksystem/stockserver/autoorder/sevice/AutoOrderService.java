package arile.toy.stocksystem.stockserver.autoorder.sevice;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoOrderService {

    private final AutoOrderRepository autoOrderRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountApiClient accountApiClient;

    public void registerAutoOrder(StockServerAutoOrderRequestEvent request) {

        long orderAmount = (long) request.orderPrice() * request.orderQuantity();

        if (request.autoOrderType() == AutoOrderType.BUY) {
            boolean reserved = accountApiClient
                    .reserveCash(request.username(), orderAmount);

            if (!reserved) {
                autoOrderResponseEventPublisher.publishError(request, AutoOrderResultCode.INSUFFICIENT_BALANCE);
                return;
            }
        } else {
            boolean reserved = accountApiClient
                    .reserveStock(request.username(), request.stockCode(), request.orderQuantity());

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
                accountApiClient.refundReservedCash(request.username(), orderAmount);
            } else {
                accountApiClient.refundReservedStock(
                        request.username(), request.stockCode(), request.orderQuantity());
            }
            autoOrderResponseEventPublisher.publishError(request, AutoOrderResultCode.INTERNAL_ERROR);
            throw e;
        }

        var autoOrderResponseMessage = new StockServerAutoOrderResponseMessage(savedAutoOrder.getAutoOrderId(),
                savedAutoOrder.getUsername(), savedAutoOrder.getStockCode(),
                savedAutoOrder.getAutoOrderType(), savedAutoOrder.getTriggerPrice(),
                savedAutoOrder.getOrderPrice(), savedAutoOrder.getOrderQuantity(),
                savedAutoOrder.getOrderTime());

        stockServerAutoOrderResponseRepository.save(autoOrderResponseMessage);
        autoOrderResponseEventPublisher.publish(autoOrderResponseMessage);
    }

    @Transactional
    public UpdateAutoOrderStatusResult updateAutoOrderStatusByCancel(Long autoOrderId) {

        AutoOrderEntity autoOrderEntity = autoOrderRepository.findByIdForUpdate(autoOrderId)
                .orElseThrow(() -> new IllegalArgumentException("auto order not found"));

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
