package arile.toy.stocksystem.bffserver.user.event.publisher;

import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUserCreatedEventPublisher implements UserCreatedEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;

    @Value("${redis.streams.user.key}")
    private String streamKey;

    public void publishUserCreatedEvent(UserCreatedEvent event) {
        Map<String, Object> payload = Map.of(
                "type", "USER_CREATED",
                "username", event.username()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("UserCreatedEvent published. recordId={}", recordId);
    }
}
