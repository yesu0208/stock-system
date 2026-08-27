package arile.toy.stocksystem.bffserver.chart.repository;

import arile.toy.stocksystem.bffserver.chart.dto.CandleData;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ChartSnapshotRepository {

    private static final String DAILY_PREFIX = "chart:daily:";
    private static final String MINUTE_PREFIX = "chart:minute:";
    private static final Duration TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveDaily(String stockCode, List<CandleData> candles) {
        try {
            redisTemplate.opsForValue().set(
                    DAILY_PREFIX + stockCode, objectMapper.writeValueAsString(candles), TTL);
        } catch (Exception e) {
            log.warn("일봉 캐시 저장 실패. stockCode={}", stockCode, e);
        }
    }

    public List<CandleData> getDaily(String stockCode) {
        String cached = redisTemplate.opsForValue().get(DAILY_PREFIX + stockCode);
        if (cached == null) return null;
        try {
            return objectMapper.readValue(cached, new TypeReference<List<CandleData>>() {});
        } catch (Exception e) {
            log.warn("일봉 캐시 역직렬화 실패. stockCode={}", stockCode, e);
            return null;
        }
    }

    public void saveMinute(String stockCode, List<MinuteCandle> candles) {
        try {
            redisTemplate.opsForValue().set(
                    MINUTE_PREFIX + stockCode, objectMapper.writeValueAsString(candles), TTL);
        } catch (Exception e) {
            log.warn("분봉 캐시 저장 실패. stockCode={}", stockCode, e);
        }
    }

    public List<MinuteCandle> getMinute(String stockCode) {
        String cached = redisTemplate.opsForValue().get(MINUTE_PREFIX + stockCode);
        if (cached == null) return null;
        try {
            return objectMapper.readValue(cached, new TypeReference<List<MinuteCandle>>() {});
        } catch (Exception e) {
            log.warn("분봉 캐시 역직렬화 실패. stockCode={}", stockCode, e);
            return null;
        }
    }
}
