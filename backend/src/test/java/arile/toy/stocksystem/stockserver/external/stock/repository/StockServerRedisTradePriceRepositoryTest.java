package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StockServerRedisTradePriceRepositoryTest {

    private ValueOperations<String, TradePriceTickMessage> valueOperations;
    private StockServerRedisTradePriceRepository repository;

    @BeforeEach
    void setUp() {
        RedisTemplate<String, TradePriceTickMessage> redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenAnswer(invocation -> valueOperations);

        repository = new StockServerRedisTradePriceRepository(redisTemplate);
    }

    @Test
    @DisplayName("Trade Price Tick 메시지를 Redis에 저장한다")
    void givenTradePriceTickMessage_whenSave_thenStoreInRedis() {
        // given
        TradePriceTickMessage message = new TradePriceTickMessage(
                TickMessageType.TRADEPRICE, "005930", Instant.now().toString(), 50000,
                500, 51000, 53000, 50000, 5, 50000,
                5000000L, 25000, 25000, "5", 30000
        );

        // when
        repository.save(message);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TradePriceTickMessage> valueCaptor = ArgumentCaptor.forClass(TradePriceTickMessage.class);

        verify(valueOperations, times(1)).set(keyCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("trade:005930");
        assertThat(valueCaptor.getValue()).isEqualTo(message);
    }
}
