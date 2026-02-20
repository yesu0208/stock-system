package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class StockServerRedisStockSummaryRepositoryTest {

    private HashOperations<String, String, StockSummaryTickMessage> hashOperations;
    private StockServerRedisStockSummaryRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, StockSummaryTickMessage> redisTemplate =
                mock(RedisTemplate.class);

        hashOperations = mock(HashOperations.class);

        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);

        repository = new StockServerRedisStockSummaryRepository(redisTemplate);
    }

    @Test
    @DisplayName("Stock Summary Tick 메시지를 Redis에 저장한다")
    void givenStockSummaryTickMessage_whenSave_thenStoreInRedis() {
        // given
        StockSummaryTickMessage message = new StockSummaryTickMessage(
                "005930",
                1000,
                2000
        );

        // when
        repository.save(message);

        // then
        verify(hashOperations, times(1)).put("stock:summary", "005930", message);
    }

    @Test
    @DisplayName("Stock Summary Tick 메시지를 Redis에서 조회한다")
    void givenStockCode_whenFindByStockCode_thenReturnMessageFromRedis() {
        // given
        StockSummaryTickMessage message = new StockSummaryTickMessage(
                "005930",
                1000,
                2000
        );
        when(hashOperations.get("stock:summary", "005930")).thenReturn(message);

        // when
        StockSummaryTickMessage result = repository.findByStockCode("005930");

        // then
        assertThat(result).isEqualTo(message);
    }
}
