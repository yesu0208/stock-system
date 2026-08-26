package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDetailRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(StockDetailTickMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel(message.stockCode()), json);
        } catch (Exception e) {
            log.warn("StockDetail 이벤트 publish 실패. stockCode={}", message.stockCode(), e);
        }
    }

    public static String channel(String stockCode) {
        return "stockdetail." + stockCode + ":event";
    }
}