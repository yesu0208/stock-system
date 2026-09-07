package arile.toy.stocksystem.bffserver.alert.event.subscriber;

import arile.toy.stocksystem.bffserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.bffserver.alert.service.AlertFiredPushService;
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
public class RedisAlertFiredEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AlertFiredPushService alertFiredPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            AlertFiredEvent event =
                    objectMapper.readValue(
                            body,
                            AlertFiredEvent.class
                    );

            alertFiredPushService.push(event);
        } catch (Exception e) {
            log.warn("AlertFiredEvent:readValue error", e);
        }
    }
}
