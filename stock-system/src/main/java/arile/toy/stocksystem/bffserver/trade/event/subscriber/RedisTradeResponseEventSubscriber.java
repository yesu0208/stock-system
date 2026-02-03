package arile.toy.stocksystem.bffserver.trade.event.subscriber;

import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.bffserver.trade.service.TradeResponsePushService;
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
public class RedisTradeResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final TradeResponsePushService tradeResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            TradeResponseEvent event =
                    objectMapper.readValue(
                            body,
                            TradeResponseEvent.class
                    );

            tradeResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("TradeResponseEvent:readValue error", e);
        }
    }
}
