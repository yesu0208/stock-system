package arile.toy.stocksystem.stockserver.useraccount.event;

import arile.toy.stocksystem.stockserver.useraccount.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUserCreatedEventConsumer {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final UserAccountService userAccountService;

    @Value("${redis.streams.user.key}")
    private String streamKey;

    @Value("${redis.streams.user.consumer-group}")
    private String group;

    private final String consumerName =
            "stock-server" + UUID.randomUUID();

    @Scheduled(fixedDelay = 1000)
    public void consume() {

        List<MapRecord<String, Object, Object>> records =
                streamRedisTemplate.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty()
                                .count(10)
                                .block(Duration.ofSeconds(5)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handle(record);
                streamRedisTemplate.opsForStream().acknowledge(
                        streamKey, group, record.getId()
                );
            } catch (Exception e) {
                log.error("Failed to process event {}", record.getId(), e);
                // Todo: 재처리 과정
            }
        }
    }

    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String type = (String) value.get("type");
        if (!"USER_CREATED".equals(type)) {
            return;
        }

        String userId = (String) value.get("username");

        userAccountService.createAccountIfAbsent(userId);

        log.info("Account created for userId={}", userId);
    }
}
