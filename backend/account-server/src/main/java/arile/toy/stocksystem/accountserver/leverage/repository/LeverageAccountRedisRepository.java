package arile.toy.stocksystem.accountserver.leverage.repository;

import arile.toy.stocksystem.accountserver.leverage.dto.LeveragePositionInfo;
import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeverageAccountRedisRepository {

    private static final String KEY_PREFIX = "account:leverage:";
    private static final String FIELD = "positions";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, LeveragePositionInfo> getPositions(String username) {
        String json = (String) redisTemplate.opsForHash().get(key(username), FIELD);
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, LeveragePositionInfo>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse leverage positions(json). username={}", username, e);
            return new HashMap<>();
        }
    }

    public void savePositions(String username, Map<String, LeveragePositionInfo> positions) {
        try {
            String json = objectMapper.writeValueAsString(positions);
            redisTemplate.opsForHash().put(key(username), FIELD, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize leverage positions. username={}", username, e);
        }
    }

    public static String positionKey(String stockCode, LeverageRatio leverageRatio) {
        return stockCode + ":" + leverageRatio.name();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
