package arile.toy.stocksystem.accountserver.trade;

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

    @Override
    public boolean applyBuyTrade(
            String username, String stockCode, int totalQuantity,
            long totalAmount, long totalCostAmount, long tradeAmount, long differenceAmount
    ) {
        return execute(buyTradeScript, username, totalQuantity, totalAmount, tradeAmount, stockCode, differenceAmount, totalCostAmount);
    }

    @Override
    public boolean applySellTrade(
            String username, String stockCode, int totalQuantity,
            long totalAmount, long totalCostAmount, long tradeAmount, long differenceAmount
    ) {
        return execute(sellTradeScript, username, totalQuantity, totalAmount, tradeAmount, stockCode, differenceAmount, totalCostAmount);
    }

    private boolean execute(
            DefaultRedisScript<Long> script, String username,
            int arg1, long arg2, long arg3, String arg4, long arg5, long arg6
    ) {
        Long result = redisTemplate.execute(
                script,
                List.of(key(username)),
                String.valueOf(arg1), String.valueOf(arg2), String.valueOf(arg3), arg4, String.valueOf(arg5), String.valueOf(arg6)
        );
        return result != null && result == 1L;
    }

    private String key(String username) {
        return "account:" + username;
    }
}
