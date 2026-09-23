package arile.toy.stocksystem.accountserver.market.holiday;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * stock-server가 원본으로 관리하는 휴장일(Redis Set "market:holidays")을
 * 주기적으로 읽어와 로컬 캐시로 들고 있음. account-server는 휴장일의
 * 원본이 아니므로, 여기서는 DB 저장 없이 Redis만 참조
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HolidayRegistry {

    private static final String KEY = "market:holidays";

    private final StringRedisTemplate redisTemplate;

    private volatile Set<LocalDate> holidays = Set.of();

    @PostConstruct
    public void init() {
        resync();
    }

    @Scheduled(fixedRate = 300_000)
    public void resync() {
        try {
            Set<String> raw = redisTemplate.opsForSet().members(KEY);
            if (raw == null) return;

            Set<LocalDate> parsed = new HashSet<>();
            for (String s : raw) {
                try {
                    parsed.add(LocalDate.parse(s));
                } catch (Exception e) {
                    log.warn("알 수 없는 휴장일 값. value={}", s);
                }
            }

            holidays = Set.copyOf(parsed);
        } catch (Exception e) {
            log.warn("휴장일 목록 재동기화 실패. 다음 주기에 재시도합니다.", e);
        }
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }
}
