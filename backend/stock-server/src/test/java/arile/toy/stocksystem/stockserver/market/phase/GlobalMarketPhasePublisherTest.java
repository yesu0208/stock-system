package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 전체 장 상태 발행 테스트")
@ExtendWith(MockitoExtension.class)
class GlobalMarketPhasePublisherTest {

    @InjectMocks private GlobalMarketPhasePublisher sut;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @DisplayName("전체 장 상태를 스냅샷 키에 저장하고 채널로 발행한다")
    @Test
    void whenPublishing_thenSavesSnapshotAndSends() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        sut.publish(StockServerMarketPhase.AFTER);

        then(valueOps).should().set("market:global-phase:snapshot", "AFTER");
        then(redisTemplate).should().convertAndSend("market:global-phase", "AFTER");
    }

    @DisplayName("스냅샷 저장이 실패해도 발행은 시도한다")
    @Test
    void givenSnapshotFails_whenPublishing_thenStillSends() {
        given(redisTemplate.opsForValue()).willThrow(new IllegalStateException("redis down"));

        sut.publish(StockServerMarketPhase.OPEN);

        then(redisTemplate).should().convertAndSend("market:global-phase", "OPEN");
    }

    @DisplayName("발행이 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishing_thenSwallows() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(redisTemplate.convertAndSend("market:global-phase", "CLOSED"))
                .willThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.publish(StockServerMarketPhase.CLOSED)).doesNotThrowAnyException();
    }
}
