package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketClosePublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private MarketClosePublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    @DisplayName("Market close 메시지를 Redis Pub/Sub으로 발행한다")
    void givenMarketClose_whenPublish_thenSendsMessage() {

        // when
        publisher.publishMarketClose();

        // then
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("market:close");
        assertThat(messageCaptor.getValue()).isEqualTo("MARKET_CLOSED");
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenRedisThrows_whenPublishMarketClose_thenDoNotThrow() {

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        assertDoesNotThrow(() -> publisher.publishMarketClose());
    }
}
