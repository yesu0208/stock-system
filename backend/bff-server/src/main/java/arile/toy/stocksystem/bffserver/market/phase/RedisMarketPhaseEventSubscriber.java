package arile.toy.stocksystem.bffserver.market.phase;

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
public class RedisMarketPhaseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final BffServerMarketPhaseRegistry registry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            MarketPhaseEvent event = objectMapper.readValue(body, MarketPhaseEvent.class);

            registry.setPhase(event.stockCode(), event.marketPhase());

            log.info("Market phase updated: {} -> {}", event.stockCode(), event.marketPhase());

        } catch (Exception e) {
            log.warn("MarketPhaseEvent readValue error", e);
        }
    }
}
