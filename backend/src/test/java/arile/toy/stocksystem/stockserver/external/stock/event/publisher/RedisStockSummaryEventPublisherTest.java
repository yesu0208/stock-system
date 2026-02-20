package arile.toy.stocksystem.stockserver.external.stock.event.publisher;

import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
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
class RedisStockSummaryEventPublisherTest {

    @Mock
    private RedisTemplate<String, StockSummaryTickEvent> redisTemplate;

    @InjectMocks
    private RedisStockSummaryEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<StockSummaryTickEvent> eventCaptor;

    @Test
    @DisplayName("Stock Summary Tick 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenStockSummaryTickEvent_whenPublish_thenSendToRedis() {

        // given
        StockSummaryTickEvent event = new StockSummaryTickEvent("005930");

        // when
        publisher.publish(event);

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("summary:event");

        assertThat(eventCaptor.getValue())
                .isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenStockSummaryTickEvent_whenPublishWithRedisException_thenDoNotThrow() {

        // given
        StockSummaryTickEvent event = new StockSummaryTickEvent("005930");

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        // when & then
        assertDoesNotThrow(() -> publisher.publish(event));
    }
}
