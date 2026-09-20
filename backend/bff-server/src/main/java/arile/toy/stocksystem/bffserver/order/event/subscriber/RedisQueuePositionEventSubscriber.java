package arile.toy.stocksystem.bffserver.order.event.subscriber;

import arile.toy.stocksystem.bffserver.order.event.QueuePositionEvent;
import arile.toy.stocksystem.bffserver.order.service.QueuePositionPushService;
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
public class RedisQueuePositionEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final QueuePositionPushService queuePositionPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            QueuePositionEvent event = objectMapper.readValue(body, QueuePositionEvent.class);
            queuePositionPushService.push(event);
        } catch (Exception e) {
            log.warn("QueuePositionEvent:readValue error", e);
        }
    }
}
