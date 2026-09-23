package arile.toy.stocksystem.accountserver.leverage.event.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterestAppliedPublisher {

    private final StringRedisTemplate redisTemplate;
    private static final String CHANNEL = "account:interest-applied";

    public void publish() {
        try {
            redisTemplate.convertAndSend(CHANNEL, "INTEREST_APPLIED");
            log.info("[InterestAppliedPublisher] Published interest applied event.");
        } catch (Exception e) {
            log.warn("publishInterestApplied error", e);
        }
    }
}
