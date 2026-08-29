package arile.toy.stocksystem.accountserver.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamGroupInitializer {

    private final StringRedisTemplate redisTemplate;

    @Value("${redis.streams.user.key}")
    private String userStreamKey;

    @Value("${redis.streams.user.consumer-group}")
    private String userConsumerGroup;

    @PostConstruct
    public void init() {
        createGroup(userStreamKey, userConsumerGroup);
    }

    private void createGroup(String streamKey, String consumerGroup) {
        try {
            redisTemplate.opsForStream()
                    .createGroup(streamKey, ReadOffset.latest(), consumerGroup);
            log.info("Consumer group created. stream={}, group={}", streamKey, consumerGroup);
        } catch (Exception e) {
            log.info("Consumer group already exists. stream={}", streamKey);
        }
    }
}
