package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

class StockServerRedisBidAskPriceRepositoryTest {

    private ValueOperations<String, BidAskPriceTickMessage> valueOperations;
    private StockServerRedisBidAskPriceRepository repository;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, BidAskPriceTickMessage> redisTemplate =
                (RedisTemplate<String, BidAskPriceTickMessage>) mock(RedisTemplate.class);

        @SuppressWarnings("unchecked")
        ValueOperations<String, BidAskPriceTickMessage> valueOps =
                (ValueOperations<String, BidAskPriceTickMessage>) mock(ValueOperations.class);

        this.valueOperations = valueOps;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        repository = new StockServerRedisBidAskPriceRepository(redisTemplate);
    }

    @Test
    @DisplayName("Bid/Ask Price Tick 메시지를 Redis에 저장한다")
    void givenBidAskPriceTickMessage_whenSave_thenStoreInRedis() {
        // given
        BidAskPriceTickMessage message = new BidAskPriceTickMessage(
                TickMessageType.BIDASKPRICE,
                "005930",
                List.of(),
                List.of(),
                1000,
                1000
        );

        // when
        repository.save(message);

        // then
        verify(valueOperations, times(1)).set("bidask:005930", message);
    }
}
