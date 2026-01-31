package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockServerRedisAccountRepository implements StockServerAccountRepository {

    private static final String KEY_PREFIX = "account:";

    private final RedisTemplate<String, StockServerAccountMessage> stockServerAccountMessageRedisTemplate;

    public void save(String username, StockServerAccountMessage account) {
        Map<String, Object> map = new HashMap<>();
        map.put("availableCash", account.availableCash());
        map.put("reservedCash", account.reservedCash());
        map.put("stocks", account.stocks());

        stockServerAccountMessageRedisTemplate.opsForHash().putAll(key(username), map);
    }

    public StockServerAccountMessage findByUsername(String username) {
        Map<Object, Object> map = stockServerAccountMessageRedisTemplate.opsForHash().entries(key(username));
        if (map == null || map.isEmpty()) return null;

        Long availableCash = map.get("availableCash") != null ? ((Number) map.get("availableCash")).longValue() : 0L;
        Long reservedCash = map.get("reservedCash") != null ? ((Number) map.get("reservedCash")).longValue() : 0L;
        Map<String, StockInfo> stocks = map.get("stocks") != null
                ? (Map<String, StockInfo>) map.get("stocks")
                : new HashMap<>();

        return new StockServerAccountMessage(username, availableCash, reservedCash, stocks);
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
