package arile.toy.stocksystem.bffserver.stockinfo.repository;

import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
@Slf4j
public class StockDetailSnapshotRepository {

    private static final String KEY_PREFIX = "stock:detail:snapshot:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(StockDetailTickMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForValue().set(key(message.stockCode()), json, TTL);
        } catch (Exception e) {
            log.warn("StockDetail 캐시 저장 실패. stockCode={}", message.stockCode(), e);
        }
    }

    public StockDetailTickMessage getLatest(String stockCode) {
        String cached = redisTemplate.opsForValue().get(key(stockCode));
        if (cached == null) return null;

        try {
            return objectMapper.readValue(cached, StockDetailTickMessage.class);
        } catch (Exception e) {
            log.warn("StockDetail 캐시 역직렬화 실패. stockCode={}", stockCode, e);
            return null;
        }
    }

    private String key(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}