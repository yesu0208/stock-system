package arile.toy.stocksystem.bffserver.order.event.publisher;

import arile.toy.stocksystem.bffserver.order.event.OrderRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisOrderStreamShardResolver;
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
public class RedisOrderRequestEventPublisher implements OrderRequestEventPublisher {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final RedisOrderStreamShardResolver shardResolver;

    @Override
    public void publishOrder(OrderRequestEvent event) {
        String streamKey = shardResolver.resolveStreamKey(event.stockCode());

        Map<String, Object> payload = Map.of(
                "type", "ORDER_CREATED",
                "username", event.username(),
                "stockCode", event.stockCode(),
                "orderType", String.valueOf(event.orderType()),
                "orderPrice", String.valueOf(event.orderPrice()),
                "orderQuantity", String.valueOf(event.orderQuantity()),
                "leverageRatio", String.valueOf(event.leverageRatio())
        );

        RecordId recordId = streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(payload)
                        .withStreamKey(streamKey)
        );

        log.info("OrderEvent published. stream={}, recordId={}",
                streamKey, recordId.getValue());
    }
}
