package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BffServerRedisStockSummaryRepositoryTest {

    @Mock
    private RedisTemplate<String, BffServerStockSummaryTickMessage> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private BffServerRedisStockSummaryRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("기존 종목 코드 조회 시 StockSummaryTickMessage 반환")
    void givenExistingStockCode_whenFindByStockCode_thenReturnsStockSummary() {
        // given
        String stockCode = "005930";
        BffServerStockSummaryTickMessage summary = new BffServerStockSummaryTickMessage(
                stockCode, 50000, 1000);
        when(hashOperations.get("stock:summary", stockCode)).thenReturn(summary);

        // when
        BffServerStockSummaryTickMessage result = repository.findByStockCode(stockCode);

        // then
        assertNotNull(result);
        assertEquals(summary, result);
        verify(hashOperations).get("stock:summary", stockCode);
    }

    @Test
    @DisplayName("존재하지 않는 종목 코드 조회 시 null 반환")
    void givenNonExistingStockCode_whenFindByStockCode_thenReturnsNull() {
        // given
        String stockCode = "000000";
        when(hashOperations.get("stock:summary", stockCode)).thenReturn(null);

        // when
        BffServerStockSummaryTickMessage result = repository.findByStockCode(stockCode);

        // then
        assertNull(result);
        verify(hashOperations).get("stock:summary", stockCode);
    }

    @Test
    @DisplayName("StockSummary 존재 시 findAll 호출하면 전체 리스트 반환")
    void givenStockSummariesExist_whenFindAll_thenReturnsListOfStockSummary() {
        // given
        BffServerStockSummaryTickMessage s1 =
                new BffServerStockSummaryTickMessage("005930", 50000, 1000);
        BffServerStockSummaryTickMessage s2 =
                new BffServerStockSummaryTickMessage("000660", 60000, 2000);
        when(hashOperations.values("stock:summary")).thenReturn(List.of(s1, s2));

        // when
        List<BffServerStockSummaryTickMessage> result = repository.findAll();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(s1));
        assertTrue(result.contains(s2));
        verify(hashOperations).values("stock:summary");
    }

    @Test
    @DisplayName("StockSummary 존재하지 않으면 findAll 호출 시 빈 리스트 반환")
    void givenNoStockSummariesExist_whenFindAll_thenReturnsEmptyList() {
        // given
        when(hashOperations.values("stock:summary")).thenReturn(List.of());

        // when
        List<BffServerStockSummaryTickMessage> result = repository.findAll();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(hashOperations).values("stock:summary");
    }
}
