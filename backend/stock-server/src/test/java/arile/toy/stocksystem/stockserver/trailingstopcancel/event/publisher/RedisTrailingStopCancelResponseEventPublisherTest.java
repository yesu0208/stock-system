package arile.toy.stocksystem.stockserver.trailingstopcancel.event.publisher;

import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 트레일링 스탑 취소 응답 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisTrailingStopCancelResponseEventPublisherTest {

    @InjectMocks private RedisTrailingStopCancelResponseEventPublisher sut;
    @Mock private RedisTemplate<String, TrailingStopCancelResponseEvent> redisTemplate;

    private final TrailingStopCancelResponseEvent event = new TrailingStopCancelResponseEvent(
            1L, "user", "005930", TrailingStopType.SELL, 67_900, 10, true, null);

    @DisplayName("사용자 채널로 이벤트를 발행한다")
    @Test
    void whenPublishing_thenSendsToUserChannel() {
        sut.publish(event);

        then(redisTemplate).should().convertAndSend("user:trailing:stop:cancel.user:event", event);
    }

    @DisplayName("Redis 발행이 실패해도 예외를 전파하지 않는다")
    @Test
    void givenRedisFails_whenPublishing_thenSwallows() {
        given(redisTemplate.convertAndSend("user:trailing:stop:cancel.user:event", event))
                .willThrow(new IllegalStateException("redis down"));

        assertThatNoException().isThrownBy(() -> sut.publish(event));
    }
}
