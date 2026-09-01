package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.LeveragePositionInfo;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountPullService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AccountSnapshot getAccountMessage(String username) {
        String key = "account:" + username;

        Map<Object, Object> accountMap = redisTemplate.opsForHash().entries(key);
        if (accountMap.isEmpty()) {
            throw new RedisAccountNotFoundException("Account not found: " + username);
        }

        long availableCash =
                Long.parseLong((String) accountMap.getOrDefault("availableCash", "0"));
        long reservedCash =
                Long.parseLong((String) accountMap.getOrDefault("reservedCash", "0"));

        String stocksJson = (String) accountMap.get("stocks");
        Map<String, StockInfo> stocks = parseOrEmpty(stocksJson, new TypeReference<Map<String, StockInfo>>() {});

        Map<String, LeveragePositionInfo> leveragePositions = getLeveragePositions(username);

        return AccountSnapshot.of(availableCash, reservedCash, stocks, leveragePositions);
    }

    private Map<String, LeveragePositionInfo> getLeveragePositions(String username) {
        String key = "account:leverage:" + username;
        String json = (String) redisTemplate.opsForHash().get(key, "positions");
        return parseOrEmpty(json, new TypeReference<Map<String, LeveragePositionInfo>>() {});
    }

    private <T> Map<String, T> parseOrEmpty(String json, TypeReference<Map<String, T>> typeReference) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse json: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
