package arile.toy.stocksystem.stockserver.trade.event.publisher;

import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTradeResponseEventPublisherTest {

    @Mock
    private RedisTemplate<String, TradeResponseEvent> redisTemplate;

    @InjectMocks
    private RedisTradeResponseEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<TradeResponseEvent> eventCaptor;

    @Test
    @DisplayName("Trade 응답 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenTradeResponseEvent_whenPublish_thenSendToRedis() {

        // given
        TradeResponseEvent event = new TradeResponseEvent(
                1L, 1L, "user1", "005930", TradeType.BUY, 50000,
                50, Instant.now()
        );

        // when
        publisher.publish(event);

        // then
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), eventCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("user:trade.user1:event");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenRedisThrows_whenPublishTradeEvent_thenDoNotThrow() {

        TradeResponseEvent event = new TradeResponseEvent(
                1L, 1L, "user1", "005930", TradeType.BUY, 50000,
                50, Instant.now()
        );

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        assertDoesNotThrow(() -> publisher.publish(event));
    }
}
