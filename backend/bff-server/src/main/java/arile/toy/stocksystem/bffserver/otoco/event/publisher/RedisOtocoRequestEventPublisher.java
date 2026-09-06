package arile.toy.stocksystem.bffserver.otoco.event.publisher;

import arile.toy.stocksystem.bffserver.otoco.event.OtocoRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisOtocoStreamShardResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisOtocoRequestEventPublisher implements OtocoRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisOtocoStreamShardResolver shardResolver;

    public void publishOtoco(OtocoRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "OTOCO_CREATED");
        payload.put("username", event.username());
        payload.put("stockCode", event.stockCode());
        payload.put("entryDirection", String.valueOf(event.entryDirection()));
        payload.put("orderQuantity", String.valueOf(event.orderQuantity()));
        payload.put("entryTriggerPrice", String.valueOf(event.entryTriggerPrice()));
        payload.put("tpMode", String.valueOf(event.tpMode()));
        payload.put("tpPrice", String.valueOf(event.tpPrice()));
        payload.put("tpPct", String.valueOf(event.tpPct()));
        payload.put("slMode", String.valueOf(event.slMode()));
        payload.put("slPrice", String.valueOf(event.slPrice()));
        payload.put("slPct", String.valueOf(event.slPct()));
        payload.put("leverageRatio", String.valueOf(event.leverageRatio()));

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("OtocoEvent published. stream={}, recordId={}", streamKey, recordId.getValue());
    }
}
