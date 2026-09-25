package arile.toy.stocksystem.stockserver.order.event.publisher;

import arile.toy.stocksystem.stockserver.order.event.QueuePositionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 대기열 순번 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisQueuePositionEventPublisherTest {

    @InjectMocks private RedisQueuePositionEventPublisher sut;

    @Mock private RedisTemplate<String, QueuePositionEvent> queuePositionEventRedisTemplate;

    @DisplayName("대기열 순번 이벤트를 주문자별 채널로 그대로 발행한다")
    @Test
    void givenEvent_whenPublishing_thenSendsToUserChannel() {
        // Given
        var event = QueuePositionEvent.of(1L, "user", "005930", 150L);

        // When
        sut.publish(event);

        // Then
        then(queuePositionEventRedisTemplate).should()
                .convertAndSend("user:order:queue-position.user:event", event);
    }

    @DisplayName("발행에 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishing_thenSwallowsException() {
        // Given
        var event = QueuePositionEvent.of(1L, "user", "005930", 0L);
        given(queuePositionEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));

        // When & Then
        assertThatCode(() -> sut.publish(event)).doesNotThrowAnyException();
    }
}
