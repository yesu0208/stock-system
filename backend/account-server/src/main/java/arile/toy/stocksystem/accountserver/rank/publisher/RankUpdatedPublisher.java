package arile.toy.stocksystem.accountserver.rank.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankUpdatedPublisher {

    private final StringRedisTemplate redisTemplate;
    private static final String CHANNEL = "account:rank-updated";

    public void publish() {
        try {
            redisTemplate.convertAndSend(CHANNEL, "RANK_UPDATED");
            log.info("[RankUpdatedPublisher] Published rank updated event.");
        } catch (Exception e) {
            log.warn("publishRankUpdated error", e);
        }
    }
}
