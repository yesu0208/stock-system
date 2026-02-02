package arile.toy.stocksystem.stockserver.useraccount.repository;

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

    public boolean reserveCash(String username, long amount) {
        return execute(reserveCashScript, username, amount);
    }

    public boolean refundReservedCash(String username, long amount) {
        return execute(refundCashScript, username, amount);
    }

    private boolean execute(DefaultRedisScript<Long> script,
                            String username,
                            long amount) {
        Long result = redisTemplate.execute(
                script,
                List.of(key(username)),
                String.valueOf(amount)
        );
        return result != null && result == 1L;
    }

    private String key(String username) {
        return "account:" + username;
    }
}

