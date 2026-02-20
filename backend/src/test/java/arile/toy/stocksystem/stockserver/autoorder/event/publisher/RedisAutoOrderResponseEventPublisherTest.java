package arile.toy.stocksystem.stockserver.autoorder.event.publisher;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAutoOrderResponseEventPublisherTest {

    @Mock
    private RedisTemplate<String, AutoOrderResponseEvent> redisTemplate;

    @InjectMocks
    private RedisAutoOrderResponseEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<AutoOrderResponseEvent> eventCaptor;

    @Test
    @DisplayName("AutoOrder 정상 응답 이벤트를 발행한다")
    void givenValidAutoOrderResponse_whenPublish_thenEventSentSuccessfully() {

        // given
        StockServerAutoOrderResponseMessage message =
                new StockServerAutoOrderResponseMessage(1L, "user1", "005930",
                        AutoOrderType.BUY, 65000, 70000, 10, Instant.now());

        // when
        publisher.publish(message);

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("user:auto:order.user1:event");

        assertThat(eventCaptor.getValue().username())
                .isEqualTo("user1");

        assertThat(eventCaptor.getValue().success())
                .isTrue();
    }

    @Test
    @DisplayName("AutoOrder 에러 이벤트를 발행한다")
    void givenAutoOrderRequestAndError_whenPublishError_thenEventSentWithError() {

        // given
        StockServerAutoOrderRequestEvent request =
                new StockServerAutoOrderRequestEvent("user1", "005930", AutoOrderType.BUY,
                        65000, 70000, 10);

        // when
        publisher.publishError(request, AutoOrderResultCode.INSUFFICIENT_BALANCE);

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("user:auto:order.user1:event");

        AutoOrderResponseEvent event = eventCaptor.getValue();

        assertThat(event.success()).isFalse();
        assertThat(event.resultCode())
                .isEqualTo(AutoOrderResultCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("AutoOrder Trigger 이벤트를 발행한다")
    void givenUsername_whenPublishTrigger_thenEventSentWithTriggeredResultCode() {

        // when
        publisher.publishTrigger("user1");

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("user:auto:order.user1:event");

        AutoOrderResponseEvent event = eventCaptor.getValue();

        assertThat(event.success()).isTrue();
        assertThat(event.resultCode())
                .isEqualTo(AutoOrderResultCode.TRIGGERED);
    }

    @Test
    @DisplayName("Redis 예외 발생해도 예외를 던지지 않는다")
    void givenRedisThrowsException_whenPublish_thenDoNotThrow() {

        // given
        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        StockServerAutoOrderResponseMessage message =
                new StockServerAutoOrderResponseMessage(1L, "user1", "005930",
                        AutoOrderType.BUY, 65000, 70000, 10, Instant.now());

        // when & then
        assertDoesNotThrow(() -> publisher.publish(message));
    }
}
