package arile.toy.stocksystem.stockserver.external.stock.event.publisher;

import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
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
class RedisTradePriceEventPublisherTest {

    @Mock
    private RedisTemplate<String, TradePriceTickEvent> redisTemplate;

    @InjectMocks
    private RedisTradePriceEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<TradePriceTickEvent> eventCaptor;

    @Test
    @DisplayName("Trade Price Tick 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenTradePriceTickEvent_whenPublish_thenSendToRedis() {

        // given
        TradePriceTickEvent event = new TradePriceTickEvent("005930");

        // when
        publisher.publish(event);

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("trade.005930:event");

        assertThat(eventCaptor.getValue())
                .isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenTradePriceTickEvent_whenPublishWithRedisException_thenDoNotThrow() {

        // given
        TradePriceTickEvent event = new TradePriceTickEvent("005930");

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        // when & then
        assertDoesNotThrow(() -> publisher.publish(event));
    }
}
