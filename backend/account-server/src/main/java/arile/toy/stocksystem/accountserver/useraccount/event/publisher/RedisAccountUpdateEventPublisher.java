package arile.toy.stocksystem.accountserver.useraccount.event.publisher;

import arile.toy.stocksystem.accountserver.useraccount.event.AccountUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAccountUpdateEventPublisher implements AccountUpdateEventPublisher {

    private final RedisTemplate<String, AccountUpdateEvent> accountUpdateEventRedisTemplate;

    public void publish(String username) {
        try {
            AccountUpdateEvent event = AccountUpdateEvent.of(username);

            String channel = resolveChannel(username);

            accountUpdateEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("accountUpdateEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:account." + username + ":event";
    }
}
