package arile.toy.stocksystem.bffserver.otoco.event.subscriber;

import arile.toy.stocksystem.bffserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.bffserver.otoco.service.OtocoResponsePushService;
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
public class RedisOtocoResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final OtocoResponsePushService otocoResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            OtocoResponseEvent event = objectMapper.readValue(body, OtocoResponseEvent.class);

            otocoResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("OtocoResponseEvent:readValue error", e);
        }
    }
}
