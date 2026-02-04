package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.*;
import arile.toy.stocksystem.stockserver.trading.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.trading.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.trading.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.trading.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoOrderService {

    private final AutoOrderRepository autoOrderRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountBalanceCommand accountBalanceCommand;

    public void registerAutoOrder(StockServerAutoOrderRequestEvent request) {

        long orderAmount = (long) request.orderPrice() * request.orderQuantity();

        boolean reserved = accountBalanceCommand
                .reserveCash(request.username(), orderAmount);

        if (!reserved) {
            autoOrderResponseEventPublisher.publishError(request, AutoOrderErrorCode.INSUFFICIENT_BALANCE);
            return;
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
            accountBalanceCommand.refundReservedCash(request.username(), orderAmount);
            autoOrderResponseEventPublisher.publishError(request, AutoOrderErrorCode.INTERNAL_ERROR);
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
}