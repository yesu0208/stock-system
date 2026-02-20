package arile.toy.stocksystem.stockserver.market.phase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisMarketPhasePublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisMarketPhasePublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    @DisplayName("MarketPhaseEvent를 Redis Pub/Sub으로 발행한다")
    void givenStockCodeAndPhase_whenPublish_thenSendsEventToRedis() throws JsonProcessingException {
        // given
        String stockCode = "005930";
        StockServerMarketPhase phase = StockServerMarketPhase.OPEN;

        MarketPhaseEvent event = MarketPhaseEvent.of(stockCode, phase);
        String json = "{\"stockCode\":\"005930\",\"phase\":\"OPEN\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(json);

        // when
        publisher.publish(stockCode, phase);

        // then
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("market-phase");
        assertThat(messageCaptor.getValue()).isEqualTo(json);
    }
}
