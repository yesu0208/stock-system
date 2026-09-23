package arile.toy.stocksystem.accountserver.lua.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLuaConfigTest {

    private final AccountLuaConfig config = new AccountLuaConfig();

    private void assertValidScript(DefaultRedisScript<Long> script) {
        assertThat(script).isNotNull();
        assertThat(script.getResultType()).isEqualTo(Long.class);
        assertThat(script.getScriptAsString()).isNotBlank();
        assertThat(script.getSha1()).isNotBlank();
    }

    @Test
    @DisplayName("reserveCashScript: availableCash를 줄이고 reservedCash를 늘린다")
    void reserveCashScript() {
        DefaultRedisScript<Long> script = config.reserveCashScript();

        assertValidScript(script);
        assertThat(script.getScriptAsString())
                .contains("availableCash", "reservedCash", "HINCRBY");
    }

    @Test
    @DisplayName("refundCashScript: reservedCash를 availableCash로 되돌린다")
    void refundCashScript() {
        DefaultRedisScript<Long> script = config.refundCashScript();

        assertValidScript(script);
        assertThat(script.getScriptAsString())
                .contains("availableCash", "reservedCash", "HINCRBY");
    }

    @Test
    @DisplayName("reserveStockScript: stocks JSON을 디코딩해 수량을 예약한다")
    void reserveStockScript() {
        DefaultRedisScript<Long> script = config.reserveStockScript();

        assertValidScript(script);
        assertThat(script.getScriptAsString()).contains("stocks", "cjson.decode");
    }

    @Test
    @DisplayName("buyTradeScript: reservedCash를 차감하고 보유 종목을 갱신한다")
    void buyTradeScript() {
        DefaultRedisScript<Long> script = config.buyTradeScript();

        assertValidScript(script);
        assertThat(script.getScriptAsString())
                .contains("reservedCash", "availableCash", "stocks", "cjson.encode");
    }

    @Test
    @DisplayName("sellTradeScript: availableCash를 증가시키고 보유 종목을 갱신/삭제한다")
    void sellTradeScript() {
        DefaultRedisScript<Long> script = config.sellTradeScript();

        assertValidScript(script);
        assertThat(script.getScriptAsString())
                .contains("availableCash", "stocks", "cjson.encode");
    }

    @Test
    void refundStockScript() {
        assertValidScript(config.refundStockScript());
    }

    @Test
    void settleLeverageBuyScript() {
        assertValidScript(config.settleLeverageBuyScript());
    }

    @Test
    void creditAvailableCashScript() {
        assertValidScript(config.creditAvailableCashScript());
    }

    @Test
    void debitAvailableCashScript() {
        assertValidScript(config.debitAvailableCashScript());
    }
}
