package arile.toy.stocksystem.bffserver.autocancel.event.publisher;

import arile.toy.stocksystem.bffserver.sharding.RedisAutoCancelStreamShardResolver;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelRequestEvent;
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
public class RedisAutoCancelRequestEventPublisher implements AutoCancelRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisAutoCancelStreamShardResolver shardResolver;

    public void publishAutoCancel(AutoCancelRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "AUTO_CANCEL_CREATED",
                "autoOrderId", String.valueOf(event.autoOrderId()),
                "stockCode", event.stockCode()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("AutoCancelEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
