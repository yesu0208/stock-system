package arile.toy.stocksystem.bffserver.leverage.event.subscriber;

import arile.toy.stocksystem.bffserver.leverage.event.MarginCallEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMarginCallEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            MarginCallEvent event = objectMapper.readValue(body, MarginCallEvent.class);

            messagingTemplate.convertAndSendToUser(event.username(), "/sub/margincall", event);
        } catch (Exception e) {
            log.warn("MarginCallEvent:readValue error", e);
        }
    }
}
