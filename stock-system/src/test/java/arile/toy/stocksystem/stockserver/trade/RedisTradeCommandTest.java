package arile.toy.stocksystem.stockserver.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTradeCommandTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private DefaultRedisScript<Long> buyTradeScript;

    @Mock
    private DefaultRedisScript<Long> sellTradeScript;

    private RedisTradeCommand redisTradeCommand;

    @BeforeEach
    void setUp() {
        redisTradeCommand =
                new RedisTradeCommand(redisTemplate, buyTradeScript, sellTradeScript);
    }

    @Test
    @DisplayName("유효한 BUY 거래일 때, Redis 실행 결과가 1이면 성공(true)")
    void givenValidBuyTrade_whenExecuteReturnsOne_thenTrue() {

        when(redisTemplate.execute(
                eq(buyTradeScript),
                eq(List.of("account:user1")),
                any(), any(), any(), any(), any()
        )).thenReturn(1L);

        boolean result = redisTradeCommand.applyBuyTrade("user1", "005930", 10,
                50000L, 500000L, 0L);

        assertTrue(result);

        verify(redisTemplate).execute(
                eq(buyTradeScript),
                eq(List.of("account:user1")),
                any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("유효한 SELL 거래일 때, Redis 실행 결과가 1이면 성공(true)")
    void givenValidSellTrade_whenExecuteReturnsOne_thenTrue() {

        when(redisTemplate.execute(
                eq(sellTradeScript),
                eq(List.of("account:user1")),
                any(), any(), any(), any(), any()
        )).thenReturn(1L);

        boolean result = redisTradeCommand.applySellTrade("user1", "005930", 5,
                45000L, 225000L, 5000L);

        assertTrue(result);
    }

    @Test
    @DisplayName("Redis 실행 결과가 0이면 실패(false)")
    void givenRedisReturnsZero_whenExecute_thenFalse() {

        when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(0L);

        boolean result = redisTradeCommand.applyBuyTrade("user1", "005930", 10,
                50000L, 500000L, 0L);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 실행 결과가 null이면 실패(false)")
    void givenRedisReturnsNull_whenExecute_thenFalse() {

        when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        boolean result = redisTradeCommand.applySellTrade("user1", "005930", 10,
                50000L, 500000L, 0L);

        assertFalse(result);
    }

    @Test
    @DisplayName("username이 null이면 실행하지 않고 실패(false)")
    void givenUsernameNull_whenExecute_thenFalse() {

        boolean result = redisTradeCommand.applyBuyTrade(null, "005930", 10,
                50000L, 500000L, 0L);

        assertFalse(result);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("script가 null이면 실행하지 않고 실패(false)")
    void givenScriptNull_whenExecute_thenFalse() {

        RedisTradeCommand command =
                new RedisTradeCommand(redisTemplate, null, sellTradeScript);

        boolean result = command.applyBuyTrade("user1", "005930", 10,
                50000L, 500000L, 0L);

        assertFalse(result);
        verifyNoInteractions(redisTemplate);
    }
}
