package arile.toy.stocksystem.stockserver.trading.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisTradeCommand implements TradeCommand {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> buyTradeScript;
    private final DefaultRedisScript<Long> sellTradeScript;

    public boolean applyBuyTrade(
            String username,
            String stockCode,
            int totalQuantity,
            long buyPrice,
            long tradeAmount,
            long differenceAmount
    ) {
        return execute(
                buyTradeScript,
                username,
                String.valueOf(totalQuantity),
                String.valueOf(buyPrice),
                String.valueOf(tradeAmount),
                stockCode,
                String.valueOf(differenceAmount)
        );
    }

    public boolean applySellTrade(
            String username,
            String stockCode,
            int totalQuantity,
            long buyPrice,
            long tradeAmount,
            long differenceAmount
    ) {
        return execute(
                sellTradeScript,
                username,
                String.valueOf(totalQuantity),
                String.valueOf(buyPrice),
                String.valueOf(tradeAmount),
                stockCode,
                String.valueOf(differenceAmount)
        );
    }

    private boolean execute(
            DefaultRedisScript<Long> script,
            String username,
            String arg1,
            String arg2,
            String arg3,
            String arg4,
            String arg5
    ) {
        if (username == null || script == null) {
            return false;
        }

        Long result = redisTemplate.execute(
                script,
                List.of(key(username)),
                arg1, arg2, arg3, arg4, arg5
        );

        return result != null && result == 1L;
    }

    private String key(String username) {
        return "account:" + username;
    }
}
