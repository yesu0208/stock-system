package arile.toy.stocksystem.bffserver.chart.event.subscriber;

import arile.toy.stocksystem.bffserver.chart.dto.DailyCandleTickMessage;
import arile.toy.stocksystem.bffserver.chart.event.DailyCandleUpdateEvent;
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
public class RedisDailyCandleEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            DailyCandleUpdateEvent event = objectMapper.readValue(body, DailyCandleUpdateEvent.class);

            messagingTemplate.convertAndSend(
                    "/sub/stock/" + event.stockCode(),
                    DailyCandleTickMessage.of(event.stockCode(), event.candle()));
        } catch (Exception e) {
            log.warn("dailyCandleUpdateEvent:readValue error", e);
        }
    }
}
