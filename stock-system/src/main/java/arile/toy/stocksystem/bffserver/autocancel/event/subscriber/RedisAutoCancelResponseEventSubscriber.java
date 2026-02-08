package arile.toy.stocksystem.bffserver.autocancel.event.subscriber;

import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.autocancel.service.AutoCancelResponsePushService;
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
public class RedisAutoCancelResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AutoCancelResponsePushService autocancelResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            AutoCancelResponseEvent event =
                    objectMapper.readValue(
                            body,
                            AutoCancelResponseEvent.class
                    );

            autocancelResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("AutoCancelResponseEvent:readValue error", e);
        }
    }
}
