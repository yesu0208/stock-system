package arile.toy.stocksystem.bffserver.autocancel.event.publisher;

import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAutoCancelStreamShardResolver;
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

        // username: stock-server에서 자동 주문 소유자 검증에 사용
        Map<String, Object> payload = Map.of(
                "type", "AUTO_CANCEL_CREATED",
                "autoOrderId", String.valueOf(event.autoOrderId()),
                "stockCode", event.stockCode(),
                "username", event.username()
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("AutoCancelEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
