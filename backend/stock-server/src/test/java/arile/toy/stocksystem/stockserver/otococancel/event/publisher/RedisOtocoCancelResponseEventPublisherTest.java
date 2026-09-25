package arile.toy.stocksystem.stockserver.otococancel.event.publisher;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] OTOCO 취소 응답 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOtocoCancelResponseEventPublisherTest {

    private static final String CHANNEL = "user:otoco:cancel.user:event";

    @InjectMocks private RedisOtocoCancelResponseEventPublisher sut;
    @Mock private RedisTemplate<String, OtocoCancelResponseEvent> redisTemplate;

    private final OtocoCancelResponseEvent event = new OtocoCancelResponseEvent(
            1L, "user", "005930", OtocoEntryDirection.BELOW, 70_000, 10, true, null);

    @DisplayName("사용자 채널로 이벤트를 발행한다")
    @Test
    void whenPublishing_thenSendsToUserChannel() {
        sut.publish(event);

        then(redisTemplate).should().convertAndSend(CHANNEL, event);
    }

    @DisplayName("Redis 발행이 실패해도 예외를 전파하지 않는다")
    @Test
    void givenRedisFails_whenPublishing_thenSwallows() {
        given(redisTemplate.convertAndSend(CHANNEL, event)).willThrow(new IllegalStateException("redis down"));

        assertThatNoException().isThrownBy(() -> sut.publish(event));
    }
}
