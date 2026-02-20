package arile.toy.stocksystem.stockserver.external.stock.event.publisher;

import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStockSummaryEventPublisher {

    private static final String CHANNEL = "summary:event";

    private final RedisTemplate<String, StockSummaryTickEvent> stockSummaryTickEventTemplate;

    public void publish(StockSummaryTickEvent stockSummaryTickEvent) {
        try {

            stockSummaryTickEventTemplate.convertAndSend(
                    CHANNEL,
                    stockSummaryTickEvent
            );
        } catch (Exception e) {
            log.warn("stockSummaryTickEventTemplate.convertAndSend error", e);
        }
    }
}
