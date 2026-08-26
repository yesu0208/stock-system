package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalMarketRedisPublisher {

    public static final String CHANNEL = "market:global:event";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(GlobalMarketResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(CHANNEL, json);
        } catch (Exception e) {
            log.warn("GlobalMarket 이벤트 publish 실패", e);
        }
    }
}