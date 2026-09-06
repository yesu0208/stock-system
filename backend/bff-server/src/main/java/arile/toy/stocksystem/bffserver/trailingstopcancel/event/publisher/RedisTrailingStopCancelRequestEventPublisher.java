package arile.toy.stocksystem.bffserver.trailingstopcancel.event.publisher;

import arile.toy.stocksystem.bffserver.sharding.RedisTrailingStopCancelStreamShardResolver;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
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
public class RedisTrailingStopCancelRequestEventPublisher implements TrailingStopCancelRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisTrailingStopCancelStreamShardResolver shardResolver;

    public void publishTrailingStopCancel(TrailingStopCancelRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "TRAILING_STOP_CANCEL_CREATED",
                "trailingStopId", String.valueOf(event.trailingStopId()),
                "stockCode", event.stockCode()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("TrailingStopCancelEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
