package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.bffserver.external.stock.service.StockSummaryPushService;
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
public class RedisStockSummaryEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final StockSummaryPushService stockSummaryPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            StockSummaryTickEvent event =
                    objectMapper.readValue(
                            body,
                            StockSummaryTickEvent.class
                    );

            stockSummaryPushService.push(event.stockCode());

        } catch (Exception e) {
            log.warn("stockSummaryTickEvent:readValue error", e);
        }
    }
}
