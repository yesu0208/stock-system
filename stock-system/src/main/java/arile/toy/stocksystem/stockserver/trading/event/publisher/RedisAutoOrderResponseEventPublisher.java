package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.trading.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.stockserver.trading.event.StockServerAutoOrderRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAutoOrderResponseEventPublisher implements AutoOrderResponseEventPublisher {

    private final RedisTemplate<String, AutoOrderResponseEvent> redisAutoOrderResponseEventRedisTemplate;

    public void publish(StockServerAutoOrderResponseMessage orderResponseMessage) {
        try {
            AutoOrderResponseEvent event =
                    AutoOrderResponseEvent.fromAutoOrderResponseMessage(orderResponseMessage, true, null);
            String channel = resolveChannel(event.username());

            redisAutoOrderResponseEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("redisAutoOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishError(StockServerAutoOrderRequestEvent orderRequestEvent, AutoOrderErrorCode orderErrorCode) {
        try {
            AutoOrderResponseEvent event = AutoOrderResponseEvent.of(
                    null, orderRequestEvent.username(), orderRequestEvent.stockCode(),
                    orderRequestEvent.autoOrderType(), orderRequestEvent.triggerPrice(),
                    orderRequestEvent.orderPrice(), orderRequestEvent.orderQuantity(), null, false,
                    orderErrorCode
            );
            String channel = resolveChannel(event.username());

            redisAutoOrderResponseEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("redisAutoOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:auto:order." + username + ":event";
    }
}
