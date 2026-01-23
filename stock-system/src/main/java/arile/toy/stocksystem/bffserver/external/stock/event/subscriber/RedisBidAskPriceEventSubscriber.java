package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.bffserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.service.BidAskPricePushService;
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
public class RedisBidAskPriceEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final BidAskPricePushService bidAskPricePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            BidAskPriceTickEvent event =
                    objectMapper.readValue(
                            body,
                            BidAskPriceTickEvent.class
                    );

            bidAskPricePushService.push(BidAskPriceTickMessage.fromEvent(event));
        } catch (Exception e) {
            log.warn("bidAskPriceTickEvent:readValue error", e);
        }
    }
}
