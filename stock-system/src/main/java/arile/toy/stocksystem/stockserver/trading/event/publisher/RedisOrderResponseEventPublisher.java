package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.dto.order.OrderErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.order.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.trading.event.OrderResponseEvent;
import arile.toy.stocksystem.stockserver.trading.event.StockServerOrderRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOrderResponseEventPublisher implements OrderResponseEventPublisher {

    private final RedisTemplate<String, OrderResponseEvent> redisOrderResponseEventRedisTemplate;

    public void publish(StockServerOrderResponseMessage orderResponseMessage) {
        try {
            OrderResponseEvent event =
                    OrderResponseEvent.fromOrderResponseMessage(orderResponseMessage, true, null);
            String channel = resolveChannel(event.username());

            redisOrderResponseEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("redisOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishError(StockServerOrderRequestEvent orderRequestEvent, OrderErrorCode orderErrorCode) {
        try {
            OrderResponseEvent event = OrderResponseEvent.of(
                    null, orderRequestEvent.username(), orderRequestEvent.stockCode(),
                    orderRequestEvent.orderType(), orderRequestEvent.orderPrice(),
                    orderRequestEvent.orderQuantity(), null, false,
                    orderErrorCode
            );
            String channel = resolveChannel(event.username());

            redisOrderResponseEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("redisOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:order." + username + ":event";
    }
}
