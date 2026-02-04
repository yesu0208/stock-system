package arile.toy.stocksystem.bffserver.autoorder.event.subscriber;

import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.service.AutoOrderResponsePushService;
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
public class RedisAutoOrderResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AutoOrderResponsePushService autoOrderResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            AutoOrderResponseEvent event =
                    objectMapper.readValue(
                            body,
                            AutoOrderResponseEvent.class
                    );

            autoOrderResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("AutoOrderResponseEvent:readValue error", e);
        }
    }
}
