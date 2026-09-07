package arile.toy.stocksystem.bffserver.alert.event.publisher;

import arile.toy.stocksystem.bffserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAlertStreamShardResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisAlertRequestEventPublisher implements AlertRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisAlertStreamShardResolver shardResolver;

    public void publishAlert(AlertRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "ALERT_CREATED",
                "username", event.username(),
                "stockCode", event.stockCode(),
                "direction", String.valueOf(event.direction()),
                "triggerPrice", String.valueOf(event.triggerPrice())
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("AlertEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
