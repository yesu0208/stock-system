package arile.toy.stocksystem.stockserver.alert.event.publisher;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDto;
import arile.toy.stocksystem.stockserver.alert.dto.AlertErrorCode;
import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;
import arile.toy.stocksystem.stockserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.event.AlertResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAlertResponseEventPublisher implements AlertResponseEventPublisher {

    private final RedisTemplate<String, AlertResponseEvent> alertResponseEventRedisTemplate;
    private final RedisTemplate<String, AlertFiredEvent> alertFiredEventRedisTemplate;

    public void publishRegistered(StockServerAlertResponseMessage message) {
        try {
            AlertResponseEvent event = AlertResponseEvent.fromResponseMessage(message);
            alertResponseEventRedisTemplate.convertAndSend(resolveChannel(event.username()), event);
        } catch (Exception e) {
            log.warn("alertResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishRegisterError(AlertRequestEvent request, AlertErrorCode errorCode) {
        try {
            AlertResponseEvent event = AlertResponseEvent.error(request, errorCode);
            alertResponseEventRedisTemplate.convertAndSend(resolveChannel(event.username()), event);
        } catch (Exception e) {
            log.warn("alertResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishFired(AlertDto alertDto, Integer currentPrice) {
        try {
            AlertFiredEvent event = AlertFiredEvent.of(alertDto, currentPrice);
            alertFiredEventRedisTemplate.convertAndSend(resolveFiredChannel(event.username()), event);
        } catch (Exception e) {
            log.warn("alertFiredEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:alert." + username + ":event";
    }

    private String resolveFiredChannel(String username) {
        return "user:alert:fired." + username + ":event";
    }
}
