package arile.toy.stocksystem.bffserver.otococancel.event.publisher;

import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisOtocoCancelStreamShardResolver;
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
public class RedisOtocoCancelRequestEventPublisher implements OtocoCancelRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisOtocoCancelStreamShardResolver shardResolver;

    public void publishOtocoCancel(OtocoCancelRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        // username: stock-server에서 OTOCO 소유자 검증에 사용
        Map<String, Object> payload = Map.of(
                "type", "OTOCO_CANCEL_CREATED",
                "otocoId", String.valueOf(event.otocoId()),
                "stockCode", event.stockCode(),
                "username", event.username()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("OtocoCancelEvent published. stream={}, recordId={}", streamKey, recordId.getValue());
    }
}
