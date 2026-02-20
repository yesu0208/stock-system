package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BffServerRedisTradePriceRepositoryTest {

    @Mock
    private RedisTemplate<String, BffServerTradePriceTickMessage> redisTemplate;

    @Mock
    private ValueOperations<String, BffServerTradePriceTickMessage> valueOperations;

    @InjectMocks
    private BffServerRedisTradePriceRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("기존 종목 코드 조회 시 TradePriceTickMessage 반환")
    void givenExistingStockCode_whenFindByStockCode_thenReturnsTradePriceTickMessage() {
        // given
        String stockCode = "005930";
        BffServerTradePriceTickMessage tick = new BffServerTradePriceTickMessage(
                TickMessageType.TRADEPRICE, stockCode, Instant.now().toString(), 50000, 1000, 49000,
                50500, 48000, 50, 50000, 25000000L, 25000,
                25000, "5", 50000
        );
        when(valueOperations.get("trade:" + stockCode)).thenReturn(tick);

        // when
        BffServerTradePriceTickMessage result = repository.findByStockCode(stockCode);

        // then
        assertNotNull(result);
        assertEquals(tick, result);
        verify(valueOperations).get("trade:" + stockCode);
    }

    @Test
    @DisplayName("존재하지 않는 종목 코드 조회 시 null 반환")
    void givenNonExistingStockCode_whenFindByStockCode_thenReturnsNull() {
        // given
        String stockCode = "000000";
        when(valueOperations.get("trade:" + stockCode)).thenReturn(null);

        // when
        BffServerTradePriceTickMessage result = repository.findByStockCode(stockCode);

        // then
        assertNull(result);
        verify(valueOperations).get("trade:" + stockCode);
    }
}
