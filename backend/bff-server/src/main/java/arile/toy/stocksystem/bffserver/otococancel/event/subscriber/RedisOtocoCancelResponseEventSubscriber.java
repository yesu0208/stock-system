package arile.toy.stocksystem.bffserver.otococancel.event.subscriber;

import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.otococancel.service.OtocoCancelResponsePushService;
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
public class RedisOtocoCancelResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final OtocoCancelResponsePushService otocoCancelResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            OtocoCancelResponseEvent event = objectMapper.readValue(body, OtocoCancelResponseEvent.class);

            otocoCancelResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("OtocoCancelResponseEvent:readValue error", e);
        }
    }
}
