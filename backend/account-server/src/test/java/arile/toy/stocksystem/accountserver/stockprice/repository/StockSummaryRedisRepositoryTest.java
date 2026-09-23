package arile.toy.stocksystem.accountserver.stockprice.repository;

import arile.toy.stocksystem.accountserver.stockprice.dto.StockSummaryTickMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class StockSummaryRedisRepositoryTest {

    private static final String KEY = "stock:summary";

    private HashOperations<String, Object, Object> hashOps;
    private StockSummaryRedisRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, StockSummaryTickMessage> redisTemplate = mock(RedisTemplate.class);
        hashOps = mock(HashOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();
        repository = new StockSummaryRedisRepository(redisTemplate);
    }

    @Test
    @DisplayName("종목 코드로 stock:summary 해시에서 주가 요약을 조회한다")
    void findByStockCode() {
        StockSummaryTickMessage summary = new StockSummaryTickMessage("005930", 70_000, 500);
        given(hashOps.get(KEY, "005930")).willReturn(summary);

        assertThat(repository.findByStockCode("005930")).isEqualTo(summary);
    }

    @Test
    @DisplayName("요약이 없으면 null을 반환한다")
    void findByStockCode_notFound() {
        given(hashOps.get(KEY, "999999")).willReturn(null);

        assertThat(repository.findByStockCode("999999")).isNull();
    }
}
