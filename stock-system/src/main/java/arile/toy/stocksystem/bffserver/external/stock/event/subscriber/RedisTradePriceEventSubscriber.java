package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.bffserver.external.stock.service.TradePricePushService;
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
public class RedisTradePriceEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final TradePricePushService tradePricePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            TradePriceTickEvent event =
                    objectMapper.readValue(
                            body,
                            TradePriceTickEvent.class
                    );

            tradePricePushService.push(event.stockCode());
        } catch (Exception e) {
            log.warn("tradePriceTickEvent:readValue error", e);
        }
    }
}
