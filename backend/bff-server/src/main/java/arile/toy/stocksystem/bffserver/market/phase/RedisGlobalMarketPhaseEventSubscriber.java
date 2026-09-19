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

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            globalMarketPhasePushService.push(body);
        } catch (Exception e) {
            log.warn("RedisGlobalMarketPhaseEventSubscriber error", e);
        }
    }
}
