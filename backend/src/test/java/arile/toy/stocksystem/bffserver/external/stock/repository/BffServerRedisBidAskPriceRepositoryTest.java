package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BffServerRedisBidAskPriceRepositoryTest {

    @Mock
    private RedisTemplate<String, BffServerBidAskPriceTickMessage> redisTemplate;

    @Mock
    private ValueOperations<String, BffServerBidAskPriceTickMessage> valueOperations;

    @InjectMocks
    private BffServerRedisBidAskPriceRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("기존 종목 코드 조회 시 BidAskPriceTickMessage 반환")
    void givenExistingStockCode_whenFindByStockCode_thenReturnsBidAskTickMessage() {
        // given
        String stockCode = "005930";
        BffServerBidAskPriceTickMessage tick = new BffServerBidAskPriceTickMessage(
                TickMessageType.BIDASKPRICE, stockCode, List.of(), List.of(), 1000, 1200
        );
        when(valueOperations.get("bidask:" + stockCode)).thenReturn(tick);

        // when
        BffServerBidAskPriceTickMessage result = repository.findByStockCode(stockCode);

        // then
        assertNotNull(result);
        assertEquals(tick, result);
        verify(valueOperations).get("bidask:" + stockCode);
    }

    @Test
    @DisplayName("존재하지 않는 종목 코드 조회 시 null 반환")
    void givenNonExistingStockCode_whenFindByStockCode_thenReturnsNull() {
        // given
        String stockCode = "000000";
        when(valueOperations.get("bidask:" + stockCode)).thenReturn(null);

        // when
        BffServerBidAskPriceTickMessage result = repository.findByStockCode(stockCode);

        // then
        assertNull(result);
        verify(valueOperations).get("bidask:" + stockCode);
    }
}
