package arile.toy.stocksystem.stockserver.market.holiday.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 휴장일 Redis 반영 테스트")
@ExtendWith(MockitoExtension.class)
class MarketHolidayRedisPublisherTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 31);

    @InjectMocks private MarketHolidayRedisPublisher sut;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SetOperations<String, String> setOps;

    @DisplayName("휴장일을 market:holidays Set에 yyyy-MM-dd 형식으로 추가한다")
    @Test
    void whenAdding_thenAddsToSet() {
        given(redisTemplate.opsForSet()).willReturn(setOps);

        sut.add(DATE);

        then(setOps).should().add("market:holidays", "2026-12-31");
    }

    @DisplayName("휴장일을 market:holidays Set에서 제거한다")
    @Test
    void whenRemoving_thenRemovesFromSet() {
        given(redisTemplate.opsForSet()).willReturn(setOps);

        sut.remove(DATE);

        then(setOps).should().remove("market:holidays", "2026-12-31");
    }

    @DisplayName("Redis 반영에 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenRedisFails_whenAddingOrRemoving_thenSwallows() {
        given(redisTemplate.opsForSet()).willThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.add(DATE)).doesNotThrowAnyException();
        assertThatCode(() -> sut.remove(DATE)).doesNotThrowAnyException();
    }
}
