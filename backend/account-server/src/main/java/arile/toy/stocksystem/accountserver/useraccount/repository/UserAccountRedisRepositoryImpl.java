package arile.toy.stocksystem.accountserver.useraccount.repository;

import arile.toy.stocksystem.accountserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.accountserver.useraccount.dto.UserAccountMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAccountRedisRepositoryImpl implements UserAccountRedisRepository {

    private static final String KEY_PREFIX = "account:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String username, UserAccountMessage account) {
        Map<String, String> map = new HashMap<>();
        map.put("availableCash", String.valueOf(account.availableCash()));
        map.put("reservedCash", String.valueOf(account.reservedCash()));
        map.put("stocks", writeStocksAsJson(account.stocks()));

        redisTemplate.opsForHash().putAll(key(username), map);
    }

    @Override
    public void saveStocks(String username, Map<String, StockInfo> stocks) {
        redisTemplate.opsForHash().put(key(username), "stocks", writeStocksAsJson(stocks));
    }

    @Override
    public UserAccountMessage findByUsername(String username) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key(username));
        if (map == null || map.isEmpty()) return null;

        Long availableCash = parseLong(map.get("availableCash"));
        Long reservedCash = parseLong(map.get("reservedCash"));
        Map<String, StockInfo> stocks = readStocksFromJson((String) map.get("stocks"));

        return new UserAccountMessage(username, availableCash, reservedCash, stocks);
    }

    @Override
    public Long getAvailableCash(String username) {
        UserAccountMessage account = findByUsername(username);
        return account != null ? account.availableCash() : 0L;
    }

    @Override
    public Long getReservedCash(String username) {
        UserAccountMessage account = findByUsername(username);
        return account != null ? account.reservedCash() : 0L;
    }

    @Override
    public Map<String, StockInfo> getStocks(String username) {
        UserAccountMessage account = findByUsername(username);
        return account != null ? account.stocks() : Collections.emptyMap();
    }

    @Override
    public void updateAccountAfterClose(String username, Long availableCash) {
        Map<String, String> map = new HashMap<>();
        map.put("availableCash", String.valueOf(availableCash));
        map.put("reservedCash", "0");
        redisTemplate.opsForHash().putAll(key(username), map);
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }

    private String writeStocksAsJson(Map<String, StockInfo> stocks) {
        try {
            return objectMapper.writeValueAsString(stocks == null ? Map.of() : stocks);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stocks", e);
            return "{}";
        }
    }

    private Map<String, StockInfo> readStocksFromJson(String stocksJson) {
        if (stocksJson == null || stocksJson.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(stocksJson, new TypeReference<Map<String, StockInfo>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse stocks(json): {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Long parseLong(Object value) {
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
