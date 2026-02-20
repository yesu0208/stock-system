package arile.toy.stocksystem.stockserver.cancel.service;

import arile.toy.stocksystem.stockserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.stockserver.cancel.entity.CancelEntity;
import arile.toy.stocksystem.stockserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.cancel.event.publisher.CancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.cancel.repository.CancelRepository;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.dto.UpdateOrderStatusResult;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelService {

    private final OrderService orderService;
    private final CancelRepository cancelRepository;
    private final OrderQueueRegistry orderQueueRegistry;
    private final CancelResponseEventPublisher cancelResponseEventPublisher;
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;
    private final AccountBalanceCommand accountBalanceCommand;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    @Transactional
    public void registerCancel(CancelRequestEvent request) {

        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(request.orderId());

        var orderEntity = result.orderEntity();

        switch (result.previousStatus()) {

            case CANCELED -> cancelResponseEventPublisher.publish(
                    CancelResponseEvent.of(orderEntity, false, CancelErrorCode.ALREADY_CANCELLED));

            case FILLED -> cancelResponseEventPublisher.publish(
                    CancelResponseEvent.of(orderEntity, false, CancelErrorCode.ALREADY_FILLED));

            default -> {

                try {
                    cancelInternal(orderEntity);
                    publishSuccess(orderEntity);

                } catch (Exception e) {

                    log.error("Cancel failed. orderId={}",
                            orderEntity.getOrderId(), e);

                    cancelResponseEventPublisher.publish(
                            CancelResponseEvent.of(orderEntity, false, CancelErrorCode.INTERNAL_ERROR));
                    throw e;
                }
            }
        }
    }

    @Transactional
    public void forceCancel(Long orderId) {

        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(orderId);

        if (!result.previousStatus().isOpen()) {
            return;
        }

        var orderEntity = result.orderEntity();

        try {
            cancelInternal(orderEntity);
            publishSuccess(orderEntity);
        } catch (Exception e) {
            log.error("Force cancel failed. orderId={}", orderId, e);
        }
    }

    private void cancelInternal(OrderEntity orderEntity) {

        orderQueueRegistry.orderCancel(
                orderEntity.getOrderId(),
                orderEntity.getStockCode()
        );

        cancelRepository.save(
                CancelEntity.of(orderEntity.getOrderId())
        );

        boolean refunded;

        if (orderEntity.getOrderType() == OrderType.BUY) {

            refunded = accountBalanceCommand.refundReservedCash(
                    orderEntity.getUsername(),
                    (long) orderEntity.getOrderPrice()
                            * orderEntity.getRemainingQuantity()
            );

            if (!refunded) {
                log.error("Redis cash refund failed. orderId={}, username={}",
                        orderEntity.getOrderId(),
                        orderEntity.getUsername());

                throw new IllegalStateException("Cash refund failed");
            }

        } else {

            refunded = accountBalanceCommand.refundReservedStock(
                    orderEntity.getUsername(),
                    orderEntity.getStockCode(),
                    orderEntity.getRemainingQuantity()
            );

            if (!refunded) {
                log.error("Redis stock refund failed. orderId={}, username={}",
                        orderEntity.getOrderId(),
                        orderEntity.getUsername());

                throw new IllegalStateException("Stock refund failed");
            }
        }
    }

    private void publishSuccess(OrderEntity orderEntity) {

        CancelResponseEvent event = CancelResponseEvent.of(orderEntity, true, null);

        stockServerOrderResponseRepository.delete(event.username(), event.orderId());

        cancelResponseEventPublisher.publish(event);
        accountUpdateEventPublisher.publish(event.username());
    }
}
