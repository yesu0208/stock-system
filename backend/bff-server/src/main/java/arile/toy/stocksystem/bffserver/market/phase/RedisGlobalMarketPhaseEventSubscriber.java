package arile.toy.stocksystem.bffserver.market.phase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisGlobalMarketPhaseEventSubscriber implements MessageListener {

    private final GlobalMarketPhasePushService globalMarketPhasePushService;
    private final BffServerMarketPhaseRegistry registry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            try {
                registry.setGlobalPhase(BffServerMarketPhase.valueOf(body));
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 global market phase 값: {}", body);
            }

            globalMarketPhasePushService.push(body);
        } catch (Exception e) {
            log.warn("RedisGlobalMarketPhaseEventSubscriber error", e);
        }
    }
}
