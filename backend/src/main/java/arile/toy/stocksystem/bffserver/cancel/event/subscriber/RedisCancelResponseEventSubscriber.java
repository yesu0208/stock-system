package arile.toy.stocksystem.bffserver.cancel.event.subscriber;

import arile.toy.stocksystem.bffserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.bffserver.cancel.service.CancelResponsePushService;
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
public class RedisCancelResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final CancelResponsePushService cancelResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            CancelResponseEvent event =
                    objectMapper.readValue(
                            body,
                            CancelResponseEvent.class
                    );

            cancelResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("CancelResponseEvent:readValue error", e);
        }
    }
}
