package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.trading.dto.cancel.CancelErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.order.*;
import arile.toy.stocksystem.stockserver.trading.entity.CancelEntity;
import arile.toy.stocksystem.stockserver.trading.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.trading.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.trading.event.publisher.CancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.repository.CancelRepository;
import arile.toy.stocksystem.stockserver.trading.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public void registerCancel(CancelRequestEvent request) {

        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(request.orderId());
        var orderEntity = result.orderEntity();

        switch (result.previousStatus()) {
            case CANCELED -> cancelResponseEventPublisher.publish(
                    CancelResponseEvent.of(orderEntity, false, CancelErrorCode.ALREADY_CANCELLED)
            );
            case FILLED -> cancelResponseEventPublisher.publish(
                    CancelResponseEvent.of(orderEntity,false, CancelErrorCode.ALREADY_FILLED)
            );
            default -> {

                orderQueueRegistry.orderCancel(orderEntity.getOrderId(), orderEntity.getStockCode());

                cancelRepository.save(CancelEntity.of(orderEntity.getOrderId()));

                if (orderEntity.getOrderType() == OrderType.BUY) {
                    boolean refunded = accountBalanceCommand.refundReservedCash(orderEntity.getUsername(), (long) orderEntity.getOrderPrice() * orderEntity.getRemainingQuantity());

                    if (!refunded) {
                        log.error("Redis refund failed. orderId={}, username={}", orderEntity.getOrderId(), orderEntity.getUsername());
                        cancelResponseEventPublisher.publish(
                                CancelResponseEvent.of(orderEntity, false, CancelErrorCode.INTERNAL_ERROR)
                        );
                        return;
                    }

                } else {
                    boolean refunded = accountBalanceCommand.refundReservedStock(orderEntity.getUsername(), orderEntity.getStockCode(),
                            orderEntity.getRemainingQuantity());

                    if (!refunded) {
                        log.error("Redis refund failed. orderId={}, username={}", orderEntity.getOrderId(), orderEntity.getUsername());
                        cancelResponseEventPublisher.publish(
                                CancelResponseEvent.of(orderEntity, false, CancelErrorCode.INTERNAL_ERROR)
                        );
                        return;
                    }
                }

                CancelResponseEvent cancelResponseEvent = CancelResponseEvent.of(orderEntity, true, null);
                stockServerOrderResponseRepository.delete(cancelResponseEvent.username(), cancelResponseEvent.orderId());
                cancelResponseEventPublisher.publish(cancelResponseEvent);
                accountUpdateEventPublisher.publish(cancelResponseEvent.username());
            }
        }
    }
}
