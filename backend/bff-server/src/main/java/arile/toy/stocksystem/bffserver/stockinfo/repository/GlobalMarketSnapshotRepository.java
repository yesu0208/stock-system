package arile.toy.stocksystem.bffserver.stockinfo.repository;

import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GlobalMarketSnapshotRepository {

    private static final String SNAPSHOT_KEY = "market:global:snapshot";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(GlobalMarketResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(SNAPSHOT_KEY, json, TTL);
        } catch (Exception e) {
            log.warn("GlobalMarketSnapshot 캐시 저장 실패", e);
        }
    }

    public GlobalMarketResponse getLatest() {
        String cached = redisTemplate.opsForValue().get(SNAPSHOT_KEY);
        if (cached == null) return null;

        try {
            return objectMapper.readValue(cached, GlobalMarketResponse.class);
        } catch (Exception e) {
            log.warn("GlobalMarketSnapshot 캐시 역직렬화 실패", e);
            return null;
        }
    }
}