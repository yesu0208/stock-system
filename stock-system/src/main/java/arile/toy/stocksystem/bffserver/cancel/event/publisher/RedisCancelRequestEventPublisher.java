package arile.toy.stocksystem.bffserver.cancel.event.publisher;

import arile.toy.stocksystem.bffserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisCancelStreamShardResolver;
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
public class RedisCancelRequestEventPublisher implements CancelRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisCancelStreamShardResolver shardResolver;

    public void publishCancel(CancelRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "CANCEL_CREATED",
                "orderId", String.valueOf(event.orderId()),
                "stockCode", event.stockCode()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("CancelEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
