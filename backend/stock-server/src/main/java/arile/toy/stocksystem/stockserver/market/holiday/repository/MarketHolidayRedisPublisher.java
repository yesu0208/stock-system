package arile.toy.stocksystem.stockserver.market.holiday.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketHolidayRedisPublisher {

    private static final String KEY = "market:holidays";

    private final StringRedisTemplate redisTemplate;

    public void add(LocalDate date) {
        try {
            redisTemplate.opsForSet().add(KEY, date.toString());
        } catch (Exception e) {
            log.warn("휴장일 Redis 추가 실패. date={}", date, e);
        }
    }

    public void remove(LocalDate date) {
        try {
            redisTemplate.opsForSet().remove(KEY, date.toString());
        } catch (Exception e) {
            log.warn("휴장일 Redis 삭제 실패. date={}", date, e);
        }
    }
}
