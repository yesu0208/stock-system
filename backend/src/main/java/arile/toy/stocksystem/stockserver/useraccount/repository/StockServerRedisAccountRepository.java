package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockServerRedisAccountRepository implements StockServerAccountRepository {

    private static final String KEY_PREFIX = "account:";

    private final RedisTemplate<String, StockServerAccountMessage> stockServerAccountMessageRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String username, StockServerAccountMessage account) {
        Map<String, Object> map = new HashMap<>();
        map.put("availableCash", account.availableCash());
        map.put("reservedCash", account.reservedCash());
        map.put("stocks", account.stocks());

        stockServerAccountMessageRedisTemplate.opsForHash().putAll(key(username), map);
    }

    @Override
    public void saveStocks(String username, Map<String, StockInfo> stocks) {
        try {
            String stocksJson = objectMapper.writeValueAsString(stocks);

            stockServerAccountMessageRedisTemplate
                    .opsForHash()
                    .put(key(username), "stocks", stocksJson);

            log.debug("Redis stocks updated for user {}: {}", username, stocksJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stocks for user {}", username, e);
        }
    }

    @Override
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

    @Override
    public Long getAvailableCash(String username) {
        StockServerAccountMessage account = findByUsername(username);
        return account != null ? account.availableCash() : 0L;
    }

    @Override
    public Long getReservedCash(String username) {
        StockServerAccountMessage account = findByUsername(username);
        return account != null ? account.reservedCash() : 0L;
    }

    @Override
    public Map<String, StockInfo> getStocks(String username) {
        StockServerAccountMessage account = findByUsername(username);
        return account != null ? account.stocks() : Collections.emptyMap();
    }

    @Override
    public void updateAccountAfterClose(String username, Long availableCash) {
        Map<String, Object> map = new HashMap<>();
        map.put("availableCash", availableCash);
        map.put("reservedCash", 0L);

        stockServerAccountMessageRedisTemplate.opsForHash().putAll(key(username), map);

        log.debug("Redis account updated after market close for user {}: availableCash={}, reservedCash=0",
                username, availableCash);
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
