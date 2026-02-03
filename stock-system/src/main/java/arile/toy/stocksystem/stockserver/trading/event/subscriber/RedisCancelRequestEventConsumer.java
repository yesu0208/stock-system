package arile.toy.stocksystem.stockserver.trading.event.subscriber;

import arile.toy.stocksystem.stockserver.trading.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.trading.service.CancelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCancelRequestEventConsumer {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final CancelService cancelService;

    @Value("${redis.streams.cancel.prefix}")
    private String prefix;

    @Value("${redis.streams.cancel.consumer-group}")
    private String group;

    @Value("${server.shard-index}")
    private int shardIndex;

    private final String consumerName =
            "stock-server" + UUID.randomUUID();

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        String streamKey = prefix + "-" + shardIndex;

        List<MapRecord<String, Object, Object>> records =
                streamRedisTemplate.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty()
                                .count(10)
                                .block(Duration.ofSeconds(5)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handle(record);
                streamRedisTemplate.opsForStream()
                        .acknowledge(streamKey, group, record.getId());
            } catch (Exception e) {
                log.error("Failed to process {}", record.getId(), e);
            }
        }
        // Todo: 재처리 과정
    }

    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String type = (String) value.get("type");
        if (!"CANCEL_CREATED".equals(type)) {
            return;
        }

        Object orderIdObj = value.get("orderId");
        Long orderId = null;
        if (orderIdObj != null) {
            if (orderIdObj instanceof Number) {
                orderId = ((Number) orderIdObj).longValue();
            } else {
                orderId = Long.parseLong(orderIdObj.toString());
            }
        }

        String stockCode = (String) value.get("stockCode");

        log.info("Processing cancel orderId: {} for stockCode {}", orderId, stockCode);

        cancelService.registerCancel(CancelRequestEvent.of(orderId, stockCode));
    }
}
