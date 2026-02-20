package arile.toy.stocksystem.stockserver.autocancel.event.publisher;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAutoCancelResponseEventPublisherTest {

    @Mock
    private RedisTemplate<String, AutoCancelResponseEvent> redisTemplate;

    @InjectMocks
    private RedisAutoCancelResponseEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<AutoCancelResponseEvent> eventCaptor;

    @Test
    @DisplayName("AutoCancelResponseEvent를 올바른 채널로 발행")
    void givenAutoCancelResponseEvent_whenPublish_thenSendToCorrectChannel() {

        // given
        AutoCancelResponseEvent event =
                new AutoCancelResponseEvent(1L, "user1", "005930", AutoOrderType.BUY,
                        50000, 50000, 50, true, null);

        // when
        publisher.publish(event);

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("user:auto:cancel.user1:event");

        assertThat(eventCaptor.getValue())
                .isEqualTo(event);
    }

    @Test
    @DisplayName("convertAndSend 중 예외 발생해도 예외를 던지지 않음")
    void givenAutoCancelResponseEvent_whenRedisThrowsException_thenDoNotThrow() {

        // given
        AutoCancelResponseEvent event =
                new AutoCancelResponseEvent(1L, "user1", "005930", AutoOrderType.BUY,
                        50000, 50000, 50, true, null);

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        // when & then
        assertDoesNotThrow(() -> publisher.publish(event));

        verify(redisTemplate).convertAndSend(
                "user:auto:cancel.user1:event",
                event
        );
    }
}
