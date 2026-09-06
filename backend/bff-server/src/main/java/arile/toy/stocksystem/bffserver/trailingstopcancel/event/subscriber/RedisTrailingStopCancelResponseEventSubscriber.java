package arile.toy.stocksystem.bffserver.trailingstopcancel.event.subscriber;

import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import arile.toy.stocksystem.bffserver.trailingstopcancel.service.TrailingStopCancelResponsePushService;
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
public class RedisTrailingStopCancelResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final TrailingStopCancelResponsePushService trailingStopCancelResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            TrailingStopCancelResponseEvent event =
                    objectMapper.readValue(
                            body,
                            TrailingStopCancelResponseEvent.class
                    );

            trailingStopCancelResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("TrailingStopCancelResponseEvent:readValue error", e);
        }
    }
}
