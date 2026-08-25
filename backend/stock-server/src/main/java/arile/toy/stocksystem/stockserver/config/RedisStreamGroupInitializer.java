package arile.toy.stocksystem.stockserver.config;

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
    private String orderConsumerGroup;

    @Value("${redis.streams.cancel.prefix}")
    private String cancelPrefix;

    @Value("${redis.streams.cancel.consumer-group}")
    private String cancelConsumerGroup;

    @Value("${redis.streams.auto-order.prefix}")
    private String autoOrderPrefix;

    @Value("${redis.streams.auto-order.consumer-group}")
    private String autoOrderConsumerGroup;

    @Value("${redis.streams.auto-cancel.prefix}")
    private String autoCancelPrefix;

    @Value("${redis.streams.auto-cancel.consumer-group}")
    private String autoCancelConsumerGroup;

    @Value("${redis.streams.user.key}")
    private String userStreamKey;

    @Value("${redis.streams.user.consumer-group}")
    private String userConsumerGroup;

    @Value("${server.group}")
    private String stockGroup;

    @PostConstruct
    public void init() {
        createGroup(orderPrefix + "-" + stockGroup, orderConsumerGroup);
        createGroup(cancelPrefix + "-" + stockGroup, cancelConsumerGroup);
        createGroup(autoOrderPrefix + "-" + stockGroup, autoOrderConsumerGroup);
        createGroup(autoCancelPrefix + "-" + stockGroup, autoCancelConsumerGroup);

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