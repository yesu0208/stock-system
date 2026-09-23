package arile.toy.stocksystem.accountserver.leverage.event.publisher;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisMarginCallEventPublisherTest {

    private static final MarginCallEvent EVENT = MarginCallEvent.of(
            "user1", "005930", LeverageRatio.X2, MarginStatus.MARGIN_CALL, 137.14);

    @Mock
    private RedisTemplate<String, MarginCallEvent> marginCallEventRedisTemplate;

    @InjectMocks
    private RedisMarginCallEventPublisher publisher;

    @Test
    @DisplayName("사용자별 마진콜 채널로 이벤트를 발행한다")
    void publish() {
        publisher.publish(EVENT);

        verify(marginCallEventRedisTemplate).convertAndSend("user:margincall.user1:event", EVENT);
    }

    @Test
    @DisplayName("발행 중 예외가 나도 호출자에게 전파하지 않는다")
    void publish_whenRedisFails_swallowsException() {
        given(marginCallEventRedisTemplate.convertAndSend("user:margincall.user1:event", EVENT))
                .willThrow(new RedisConnectionFailureException("connection refused"));

        assertThatCode(() -> publisher.publish(EVENT)).doesNotThrowAnyException();
    }
}
