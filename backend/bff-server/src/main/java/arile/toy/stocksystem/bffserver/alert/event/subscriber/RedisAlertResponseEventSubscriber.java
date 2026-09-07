package arile.toy.stocksystem.bffserver.alert.event.subscriber;

import arile.toy.stocksystem.bffserver.alert.event.AlertResponseEvent;
import arile.toy.stocksystem.bffserver.alert.service.AlertResponsePushService;
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
public class RedisAlertResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AlertResponsePushService alertResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            AlertResponseEvent event =
                    objectMapper.readValue(
                            body,
                            AlertResponseEvent.class
                    );

            alertResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("AlertResponseEvent:readValue error", e);
        }
    }
}
