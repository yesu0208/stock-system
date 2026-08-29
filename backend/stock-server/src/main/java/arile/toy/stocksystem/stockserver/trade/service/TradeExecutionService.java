package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.stockserver.trade.event.publisher.TradeResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trade.outbox.service.TradeOutboxRecorder;
import arile.toy.stocksystem.stockserver.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionService {

    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;
    private final TradeResponseEventPublisher tradeResponseEventPublisher;
    private final TradeOutboxRecorder tradeOutboxRecorder;

    @Transactional
    public TradeResult executeBuyTrade(OrderDto buyOrderDto, int tradePrice, int executable) {
        return execute(buyOrderDto, tradePrice, executable, TradeType.BUY);
    }

    @Transactional
    public TradeResult executeSellTrade(OrderDto sellOrderDto, int tradePrice, int executable) {
        return execute(sellOrderDto, tradePrice, executable, TradeType.SELL);
    }

    private TradeResult execute(OrderDto orderDto, int tradePrice, int executable, TradeType tradeType) {

        OrderEntity orderEntity = orderRepository.findByIdForUpdate(orderDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (orderEntity.getOrderStatus() != OrderStatus.OPEN &&
                orderEntity.getOrderStatus() != OrderStatus.PARTIAL) {
            return null;
        }

        TradeEntity tradeEntity = tradeRepository.save(
                TradeEntity.of(orderDto.orderId(), orderDto.username(),
                        orderDto.stockCode(), tradeType, tradePrice, executable)
        );

        int remainingQuantity = orderDto.remainingQuantity() - executable;
        orderEntity.setOrderStatus(remainingQuantity > 0 ? OrderStatus.PARTIAL : OrderStatus.FILLED);
        orderEntity.setRemainingQuantity(remainingQuantity);
        orderRepository.save(orderEntity);

        tradeOutboxRecorder.record(
                TradeExecutedEvent.of(
                        tradeEntity.getTradeId(), orderDto.orderId(), orderDto.username(),
                        orderDto.stockCode(), tradeType, orderDto.orderPrice(), tradePrice, executable
                )
        );

        tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(tradeEntity));

        return TradeResult.of(tradeEntity);
    }
}
