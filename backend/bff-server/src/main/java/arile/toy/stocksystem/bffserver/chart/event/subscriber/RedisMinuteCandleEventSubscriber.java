package arile.toy.stocksystem.bffserver.chart.event.subscriber;

import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandleTickMessage;
import arile.toy.stocksystem.bffserver.chart.event.MinuteCandleUpdateEvent;
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
public class RedisMinuteCandleEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            MinuteCandleUpdateEvent event = objectMapper.readValue(body, MinuteCandleUpdateEvent.class);

            messagingTemplate.convertAndSend(
                    "/sub/stock/" + event.stockCode(),
                    MinuteCandleTickMessage.of(event.stockCode(), event.candle()));
        } catch (Exception e) {
            log.warn("minuteCandleUpdateEvent:readValue error", e);
        }
    }
}
