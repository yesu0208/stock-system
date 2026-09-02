package arile.toy.stocksystem.stockserver.autoorder.event.publisher;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
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

    public void publishError(StockServerAutoOrderRequestEvent orderRequestEvent, AutoOrderResultCode orderErrorCode) {
        try {
            AutoOrderResponseEvent event = AutoOrderResponseEvent.of(
                    null, orderRequestEvent.username(), orderRequestEvent.stockCode(),
                    orderRequestEvent.autoOrderType(), orderRequestEvent.leverageRatio(), orderRequestEvent.triggerPrice(),
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

    public void publishTrigger(String username) {
        try {
            AutoOrderResponseEvent event = AutoOrderResponseEvent.of(
                    null, username, null,
                    null, null,
                    null, null, null, null,true,
                    AutoOrderResultCode.TRIGGERED
            );

            String channel = resolveChannel(username);

            redisAutoOrderResponseEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("redisAutoOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishTriggerFailure(AutoOrderDto autoOrderDto, AutoOrderResultCode resultCode) {
        try {
            AutoOrderResponseEvent event = AutoOrderResponseEvent.of(
                    autoOrderDto.autoOrderId(),
                    autoOrderDto.username(),
                    autoOrderDto.stockCode(),
                    autoOrderDto.autoOrderType(),
                    autoOrderDto.leverageRatio(),
                    autoOrderDto.triggerPrice(),
                    autoOrderDto.orderPrice(),
                    autoOrderDto.orderQuantity(),
                    null,
                    false,
                    resultCode
            );

            String channel = resolveChannel(event.username());
            redisAutoOrderResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisAutoOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:auto:order." + username + ":event";
    }
}
