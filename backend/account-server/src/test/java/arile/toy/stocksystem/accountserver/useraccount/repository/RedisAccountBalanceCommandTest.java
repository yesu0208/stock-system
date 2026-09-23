package arile.toy.stocksystem.accountserver.useraccount.repository;

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

class RedisAccountBalanceCommandTest {

    private static final String USERNAME = "user1";
    private static final List<String> KEYS = List.of("account:" + USERNAME);

    private StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> reserveCashScript;
    private DefaultRedisScript<Long> refundCashScript;
    private DefaultRedisScript<Long> reserveStockScript;
    private DefaultRedisScript<Long> refundStockScript;
    private DefaultRedisScript<Long> settleLeverageBuyScript;
    private DefaultRedisScript<Long> creditAvailableCashScript;
    private DefaultRedisScript<Long> debitAvailableCashScript;

    private RedisAccountBalanceCommand command;

    private static DefaultRedisScript<Long> script(String name) {
        return new DefaultRedisScript<>("return '" + name + "'", Long.class);
    }

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        reserveCashScript = script("reserveCash");
        refundCashScript = script("refundCash");
        reserveStockScript = script("reserveStock");
        refundStockScript = script("refundStock");
        settleLeverageBuyScript = script("settleLeverageBuy");
        creditAvailableCashScript = script("creditAvailableCash");
        debitAvailableCashScript = script("debitAvailableCash");

        command = new RedisAccountBalanceCommand(
                redisTemplate,
                reserveCashScript,
                refundCashScript,
                reserveStockScript,
                refundStockScript,
                settleLeverageBuyScript,
                creditAvailableCashScript,
                debitAvailableCashScript
        );
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("reserveCash: 금액을 인자로 reserveCashScript를 실행한다")
    void reserveCash(Long result, boolean expected) {
        given(redisTemplate.execute(eq(reserveCashScript), eq(KEYS), eq("1000")))
                .willReturn(result);

        assertThat(command.reserveCash(USERNAME, 1000L)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("refundReservedCash: 금액을 인자로 refundCashScript를 실행한다")
    void refundReservedCash(Long result, boolean expected) {
        given(redisTemplate.execute(eq(refundCashScript), eq(KEYS), eq("500")))
                .willReturn(result);

        assertThat(command.refundReservedCash(USERNAME, 500L)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("reserveStock: 수량, 종목코드를 인자로 reserveStockScript를 실행한다")
    void reserveStock(Long result, boolean expected) {
        given(redisTemplate.execute(eq(reserveStockScript), eq(KEYS), eq("10"), eq("005930")))
                .willReturn(result);

        assertThat(command.reserveStock(USERNAME, "005930", 10)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("refundReservedStock: 수량, 종목코드를 인자로 refundStockScript를 실행한다")
    void refundReservedStock(Long result, boolean expected) {
        given(redisTemplate.execute(eq(refundStockScript), eq(KEYS), eq("5"), eq("005930")))
                .willReturn(result);

        assertThat(command.refundReservedStock(USERNAME, "005930", 5)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("settleLeverageBuy: 예약 차감액, 가용 증가액을 인자로 실행한다")
    void settleLeverageBuy(Long result, boolean expected) {
        given(redisTemplate.execute(eq(settleLeverageBuyScript), eq(KEYS), eq("3000"), eq("1000")))
                .willReturn(result);

        assertThat(command.settleLeverageBuy(USERNAME, 3000L, 1000L)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("creditAvailableCash: 금액을 인자로 creditAvailableCashScript를 실행한다")
    void creditAvailableCash(Long result, boolean expected) {
        given(redisTemplate.execute(eq(creditAvailableCashScript), eq(KEYS), eq("700")))
                .willReturn(result);

        assertThat(command.creditAvailableCash(USERNAME, 700L)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "스크립트 결과={0} → {1}")
    @CsvSource({"1, true", "0, false", ", false"})
    @DisplayName("debitAvailableCash: 금액을 인자로 debitAvailableCashScript를 실행한다")
    void debitAvailableCash(Long result, boolean expected) {
        given(redisTemplate.execute(eq(debitAvailableCashScript), eq(KEYS), eq("200")))
                .willReturn(result);

        assertThat(command.debitAvailableCash(USERNAME, 200L)).isEqualTo(expected);
    }
}
