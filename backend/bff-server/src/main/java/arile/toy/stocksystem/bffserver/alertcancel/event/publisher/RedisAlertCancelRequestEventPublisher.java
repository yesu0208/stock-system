package arile.toy.stocksystem.bffserver.alertcancel.event.publisher;

import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAlertCancelStreamShardResolver;
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
public class RedisAlertCancelRequestEventPublisher implements AlertCancelRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisAlertCancelStreamShardResolver shardResolver;

    public void publishAlertCancel(AlertCancelRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "ALERT_CANCEL_CREATED",
                "alertId", String.valueOf(event.alertId()),
                "stockCode", event.stockCode()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("AlertCancelEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
