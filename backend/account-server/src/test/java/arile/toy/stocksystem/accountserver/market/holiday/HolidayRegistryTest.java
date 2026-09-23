package arile.toy.stocksystem.accountserver.market.holiday;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class HolidayRegistryTest {

    private static final String KEY = "market:holidays";
    private static final LocalDate CHUSEOK = LocalDate.of(2026, 9, 24);
    private static final LocalDate CHUSEOK_NEXT = LocalDate.of(2026, 9, 25);
    private static final LocalDate HANGUL_DAY = LocalDate.of(2026, 10, 9);

    private SetOperations<String, String> setOps;
    private HolidayRegistry registry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        setOps = mock(SetOperations.class);
        doReturn(setOps).when(redisTemplate).opsForSet();
        registry = new HolidayRegistry(redisTemplate);
    }

    @Test
    @DisplayName("init: 시작 시 Redis에서 휴장일을 읽어 캐시에 올린다")
    void init_loadsHolidays() {
        given(setOps.members(KEY)).willReturn(Set.of("2026-09-24", "2026-09-25"));

        registry.init();

        assertThat(registry.isHoliday(CHUSEOK)).isTrue();
        assertThat(registry.isHoliday(CHUSEOK_NEXT)).isTrue();
        assertThat(registry.isHoliday(HANGUL_DAY)).isFalse();
    }

    @Test
    @DisplayName("resync: 새 목록으로 통째로 교체하여, Redis에서 삭제된 휴장일은 캐시에서도 빠진다")
    void resync_replacesCache() {
        given(setOps.members(KEY)).willReturn(Set.of("2026-09-24", "2026-09-25"), Set.of("2026-10-09"));

        registry.resync();
        registry.resync();

        assertThat(registry.isHoliday(CHUSEOK)).isFalse();
        assertThat(registry.isHoliday(CHUSEOK_NEXT)).isFalse();
        assertThat(registry.isHoliday(HANGUL_DAY)).isTrue();
    }

    @Test
    @DisplayName("resync: 날짜로 해석할 수 없는 값은 건너뛰고 나머지는 반영한다")
    void resync_skipsInvalidValues() {
        given(setOps.members(KEY)).willReturn(Set.of("2026-09-24", "not-a-date", "2026/10/09"));

        registry.resync();

        assertThat(registry.isHoliday(CHUSEOK)).isTrue();
        assertThat(registry.isHoliday(HANGUL_DAY)).isFalse();
    }

    @Test
    @DisplayName("resync: Redis 결과가 null이면 기존 캐시를 유지한다")
    void resync_whenNull_keepsCache() {
        given(setOps.members(KEY)).willReturn(Set.of("2026-09-24"), (Set<String>) null);

        registry.resync();
        registry.resync();

        assertThat(registry.isHoliday(CHUSEOK)).isTrue();
    }

    @Test
    @DisplayName("resync: Redis 장애가 나도 예외를 전파하지 않고 기존 캐시를 유지한다")
    void resync_whenRedisFails_keepsCache() {
        given(setOps.members(KEY))
                .willReturn(Set.of("2026-09-24"))
                .willThrow(new RedisConnectionFailureException("connection refused"));

        registry.resync();
        assertThatCode(() -> registry.resync()).doesNotThrowAnyException();

        assertThat(registry.isHoliday(CHUSEOK)).isTrue();
    }

    @Test
    @DisplayName("resync: Redis에 휴장일이 하나도 없으면 캐시를 비운다")
    void resync_whenEmpty_clearsCache() {
        given(setOps.members(KEY)).willReturn(Set.of("2026-09-24"), Set.of());

        registry.resync();
        registry.resync();

        assertThat(registry.isHoliday(CHUSEOK)).isFalse();
    }
}
