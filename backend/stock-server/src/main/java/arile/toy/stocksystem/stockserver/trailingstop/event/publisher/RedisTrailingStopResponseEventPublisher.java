package arile.toy.stocksystem.stockserver.trailingstop.event.publisher;

import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstop.event.TrailingStopResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTrailingStopResponseEventPublisher implements TrailingStopResponseEventPublisher {

    private final RedisTemplate<String, TrailingStopResponseEvent> redisTrailingStopResponseEventRedisTemplate;

    public void publish(StockServerTrailingStopResponseMessage message) {
        try {
            TrailingStopResponseEvent event = TrailingStopResponseEvent.fromResponseMessage(message, true, null);
            String channel = resolveChannel(event.username());

            redisTrailingStopResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisTrailingStopResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishError(StockServerTrailingStopRequestEvent request, TrailingStopResultCode resultCode) {
        try {
            TrailingStopResponseEvent event = TrailingStopResponseEvent.of(
                    null, request.username(), request.stockCode(), request.trailingStopType(), request.leverageRatio(),
                    request.orderQuantity(), request.stopPercent(), request.basePrice(), null, null,
                    false, resultCode
            );
            String channel = resolveChannel(event.username());

            redisTrailingStopResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisTrailingStopResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishTrigger(String username) {
        try {
            TrailingStopResponseEvent event = TrailingStopResponseEvent.of(
                    null, username, null, null, null,
                    null, null, null, null, null,
                    true, TrailingStopResultCode.TRIGGERED
            );
            String channel = resolveChannel(username);

            redisTrailingStopResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisTrailingStopResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishTriggerFailure(TrailingStopDto dto, TrailingStopResultCode resultCode) {
        try {
            TrailingStopResponseEvent event = TrailingStopResponseEvent.of(
                    dto.trailingStopId(), dto.username(), dto.stockCode(), dto.trailingStopType(), dto.leverageRatio(),
                    dto.orderQuantity(), dto.stopPercent(), dto.basePrice(), dto.triggerPrice(), null,
                    false, resultCode
            );
            String channel = resolveChannel(event.username());

            redisTrailingStopResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisTrailingStopResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishTrailingUpdate(TrailingStopDto dto) {
        try {
            TrailingStopResponseEvent event = TrailingStopResponseEvent.of(
                    dto.trailingStopId(), dto.username(), dto.stockCode(), dto.trailingStopType(), dto.leverageRatio(),
                    dto.orderQuantity(), dto.stopPercent(), dto.basePrice(), dto.triggerPrice(), dto.orderTime(),
                    true, TrailingStopResultCode.TRAILING_UPDATED
            );
            String channel = resolveChannel(event.username());

            redisTrailingStopResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisTrailingStopResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:trailing:stop." + username + ":event";
    }
}
