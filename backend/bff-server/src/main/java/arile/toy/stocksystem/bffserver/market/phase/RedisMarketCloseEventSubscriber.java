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
public class RedisMarketCloseEventSubscriber implements MessageListener {

    private final MarketCloseEventPushService marketCloseEventPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            marketCloseEventPushService.push(body);

        } catch (Exception e) {
            log.warn("RedisMarketCloseEventSubscriber error", e);
        }
    }
}
