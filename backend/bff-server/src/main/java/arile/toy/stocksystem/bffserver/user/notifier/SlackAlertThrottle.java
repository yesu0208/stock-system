package arile.toy.stocksystem.bffserver.user.notifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 짧은 시간 내 동일 예외 타입이 반복 발생할 때 Slack 알림 폭주를 막음.
 * Redis SETNX(setIfAbsent) + TTL 기반이라 인스턴스가 여러 개로 늘어나도
 * 전체 인스턴스를 통틀어 정확히 "TTL초에 1건"이 보장.
 * (setIfAbsent는 원자적 연산이라 동시에 여러 인스턴스가 호출해도 하나만 성공함).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlackAlertThrottle {

    private static final String KEY_PREFIX = "slack:throttle:";
    private static final Duration SUPPRESS_WINDOW = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public boolean shouldSend(String key) {
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + key, "1", SUPPRESS_WINDOW);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {

            log.warn("Slack 억제 판단 중 Redis 오류. key={} — 알림은 그대로 진행", key, e);
            return true;
        }
    }
}
