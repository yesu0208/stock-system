package arile.toy.stocksystem.bffserver.leverage.event.subscriber;

import arile.toy.stocksystem.bffserver.leverage.event.LiquidationExecutedEvent;
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
public class RedisLiquidationEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            LiquidationExecutedEvent event = objectMapper.readValue(body, LiquidationExecutedEvent.class);

            messagingTemplate.convertAndSendToUser(event.username(), "/sub/liquidation", event);
        } catch (Exception e) {
            log.warn("LiquidationExecutedEvent:readValue error", e);
        }
    }
}
