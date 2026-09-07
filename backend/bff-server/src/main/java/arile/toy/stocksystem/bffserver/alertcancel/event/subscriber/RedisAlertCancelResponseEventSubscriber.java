package arile.toy.stocksystem.bffserver.alertcancel.event.subscriber;

import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelResponseEvent;
import arile.toy.stocksystem.bffserver.alertcancel.service.AlertCancelResponsePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAlertCancelResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AlertCancelResponsePushService alertCancelResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            AlertCancelResponseEvent event =
                    objectMapper.readValue(
                            body,
                            AlertCancelResponseEvent.class
                    );

            alertCancelResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("AlertCancelResponseEvent:readValue error", e);
        }
    }
}
