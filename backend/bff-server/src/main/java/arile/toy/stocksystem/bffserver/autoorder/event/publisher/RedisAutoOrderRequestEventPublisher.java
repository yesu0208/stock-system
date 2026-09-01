package arile.toy.stocksystem.bffserver.autoorder.event.publisher;

import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAutoOrderStreamShardResolver;
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
public class RedisAutoOrderRequestEventPublisher implements AutoOrderRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisAutoOrderStreamShardResolver shardResolver;

    public void publishAutoOrder(AutoOrderRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "AUTO_ORDER_CREATED",
                "username", event.username(),
                "stockCode", event.stockCode(),
                "autoOrderType", String.valueOf(event.autoOrderType()),
                "triggerPrice", String.valueOf(event.triggerPrice()),
                "orderPrice", String.valueOf(event.orderPrice()),
                "orderQuantity", String.valueOf(event.orderQuantity()),
                "leverageRatio", String.valueOf(event.leverageRatio())
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("AutoOrderEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
