package arile.toy.stocksystem.accountserver.useraccount.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisAccountBalanceCommand implements AccountBalanceCommand {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveCashScript;
    private final DefaultRedisScript<Long> refundCashScript;
    private final DefaultRedisScript<Long> reserveStockScript;
    private final DefaultRedisScript<Long> refundStockScript;
    private final DefaultRedisScript<Long> settleLeverageBuyScript;
    private final DefaultRedisScript<Long> creditAvailableCashScript;
    private final DefaultRedisScript<Long> debitAvailableCashScript;

    public boolean reserveCash(String username, long amount) {
        return execute(reserveCashScript, username, amount);
    }

    public boolean refundReservedCash(String username, long amount) {
        return execute(refundCashScript, username, amount);
    }

    public boolean reserveStock(String username, String stockCode, int quantity) {
        return execute(reserveStockScript, username, quantity, stockCode);
    }

    public boolean refundReservedStock(String username, String stockCode, int quantity) {
        return execute(refundStockScript, username, quantity, stockCode);
    }

    public boolean settleLeverageBuy(String username, long reservedDecrease, long availableIncrease) {
        Long result = redisTemplate.execute(
                settleLeverageBuyScript,
                List.of(key(username)),
                String.valueOf(reservedDecrease),
                String.valueOf(availableIncrease)
        );
        return result != null && result == 1L;
    }

    public boolean creditAvailableCash(String username, long amount) {
        Long result = redisTemplate.execute(
                creditAvailableCashScript,
                List.of(key(username)),
                String.valueOf(amount)
        );
        return result != null && result == 1L;
    }

    @Override
    public boolean debitAvailableCash(String username, long amount) {
        Long result = redisTemplate.execute(
                debitAvailableCashScript, List.of(key(username)), String.valueOf(amount));
        return result != null && result == 1L;
    }

    private boolean execute(DefaultRedisScript<Long> script, String username, int quantity, String stockCode) {
        Long result = redisTemplate.execute(
                script, List.of(key(username)), String.valueOf(quantity), stockCode);
        return result != null && result == 1L;
    }

    private boolean execute(DefaultRedisScript<Long> script, String username, long amount) {
        Long result = redisTemplate.execute(
                script, List.of(key(username)), String.valueOf(amount));
        return result != null && result == 1L;
    }

    private String key(String username) {
        return "account:" + username;
    }
}
