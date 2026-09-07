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
    
    @Value("${redis.streams.trailing-stop.prefix}")
    private String trailingStopPrefix;

    @Value("${redis.streams.trailing-stop.consumer-group}")
    private String trailingStopConsumerGroup;

    @Value("${redis.streams.trailing-stop-cancel.prefix}")
    private String trailingStopCancelPrefix;

    @Value("${redis.streams.trailing-stop-cancel.consumer-group}")
    private String trailingStopCancelConsumerGroup;

    @Value("${redis.streams.otoco.prefix}")
    private String otocoPrefix;

    @Value("${redis.streams.otoco.consumer-group}")
    private String otocoConsumerGroup;

    @Value("${redis.streams.otoco-cancel.prefix}")
    private String otocoCancelPrefix;

    @Value("${redis.streams.otoco-cancel.consumer-group}")
    private String otocoCancelConsumerGroup;

    @Value("${redis.streams.alert.prefix}")
    private String alertPrefix;

    @Value("${redis.streams.alert.consumer-group}")
    private String alertConsumerGroup;

    @Value("${redis.streams.alert-cancel.prefix}")
    private String alertCancelPrefix;

    @Value("${redis.streams.alert-cancel.consumer-group}")
    private String alertCancelConsumerGroup;

    @Value("${server.group}")
    private String stockGroup;

    @PostConstruct
    public void init() {
        createGroup(orderPrefix + "-" + stockGroup, orderConsumerGroup);
        createGroup(cancelPrefix + "-" + stockGroup, cancelConsumerGroup);
        createGroup(autoOrderPrefix + "-" + stockGroup, autoOrderConsumerGroup);
        createGroup(autoCancelPrefix + "-" + stockGroup, autoCancelConsumerGroup);
        createGroup(trailingStopPrefix + "-" + stockGroup, trailingStopConsumerGroup);
        createGroup(trailingStopCancelPrefix + "-" + stockGroup, trailingStopCancelConsumerGroup);
        createGroup(otocoPrefix + "-" + stockGroup, otocoConsumerGroup);
        createGroup(otocoCancelPrefix + "-" + stockGroup, otocoCancelConsumerGroup);
        createGroup(alertPrefix + "-" + stockGroup, alertConsumerGroup);
        createGroup(alertCancelPrefix + "-" + stockGroup, alertCancelConsumerGroup);
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
