package arile.toy.stocksystem.accountserver.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RedisTradeCommandTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final List<String> KEYS = List.of("account:" + USERNAME);

    private StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> buyTradeScript;
    private DefaultRedisScript<Long> sellTradeScript;

    private RedisTradeCommand command;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        buyTradeScript = new DefaultRedisScript<>("return 'buy'", Long.class);
        sellTradeScript = new DefaultRedisScript<>("return 'sell'", Long.class);
        command = new RedisTradeCommand(redisTemplate, buyTradeScript, sellTradeScript);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("applyBuyTrade: ARGV 순서(수량, 총액, 체결액, 종목, 차액, 원가)대로 buyTradeScript를 실행한다")
    void applyBuyTrade(Long result, boolean expected) {
        given(redisTemplate.execute(
                eq(buyTradeScript), eq(KEYS),
                eq("10"), eq("700000"), eq("700000"), eq(STOCK_CODE), eq("5000"), eq("705000")
        )).willReturn(result);

        boolean actual = command.applyBuyTrade(
                USERNAME, STOCK_CODE, 10, 700000L, 705000L, 700000L, 5000L);

        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("applySellTrade: ARGV 순서(수량, 총액, 체결액, 종목, 차액, 원가)대로 sellTradeScript를 실행한다")
    void applySellTrade(Long result, boolean expected) {
        given(redisTemplate.execute(
                eq(sellTradeScript), eq(KEYS),
                eq("3"), eq("210000"), eq("280000"), eq(STOCK_CODE), eq("-1000"), eq("211000")
        )).willReturn(result);

        boolean actual = command.applySellTrade(
                USERNAME, STOCK_CODE, 3, 210000L, 211000L, 280000L, -1000L);

        assertThat(actual).isEqualTo(expected);
    }
}
