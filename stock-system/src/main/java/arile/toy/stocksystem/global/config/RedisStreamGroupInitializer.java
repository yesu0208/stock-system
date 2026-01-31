package arile.toy.stocksystem.global.config;

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

    @Value("${redis.streams.order.prefix}")
    private String orderPrefix;

    @Value("${redis.streams.order.consumer-group}")
    private String orderGroup;

    @Value("${server.shard-index}")
    private int shardIndex;

    @PostConstruct
    public void init() {
        createGroup(orderPrefix + "-" + shardIndex, orderGroup);
    }

    private void createGroup(String streamKey, String group) {
        try {
            redisTemplate.opsForStream()
                    .createGroup(streamKey, ReadOffset.latest(), group);
            log.info("Consumer group created. stream={}, group={}", streamKey, group);
        } catch (Exception e) {
            log.info("Consumer group already exists. stream={}", streamKey);
        }
    }
}
