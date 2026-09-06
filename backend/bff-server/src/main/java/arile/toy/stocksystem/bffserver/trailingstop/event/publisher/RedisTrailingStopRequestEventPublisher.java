package arile.toy.stocksystem.bffserver.trailingstop.event.publisher;

import arile.toy.stocksystem.bffserver.sharding.RedisTrailingStopStreamShardResolver;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopRequestEvent;
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
public class RedisTrailingStopRequestEventPublisher implements TrailingStopRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisTrailingStopStreamShardResolver shardResolver;

    public void publishTrailingStop(TrailingStopRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "TRAILING_STOP_CREATED",
                "username", event.username(),
                "stockCode", event.stockCode(),
                "trailingStopType", String.valueOf(event.trailingStopType()),
                "orderQuantity", String.valueOf(event.orderQuantity()),
                "stopPercent", String.valueOf(event.stopPercent()),
                "basePrice", String.valueOf(event.basePrice()),
                "leverageRatio", String.valueOf(event.leverageRatio())
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("TrailingStopEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
