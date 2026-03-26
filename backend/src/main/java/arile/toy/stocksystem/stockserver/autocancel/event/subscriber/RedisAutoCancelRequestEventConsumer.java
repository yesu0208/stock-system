package arile.toy.stocksystem.stockserver.autocancel.event.subscriber;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
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
public class RedisAutoCancelRequestEventConsumer {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final AutoCancelService autoCancelService;
    private final StockServerMarketPhaseRegistry registry;

    @Value("${redis.streams.auto-cancel.prefix}")
    private String prefix;

    @Value("${redis.streams.auto-cancel.consumer-group}")
    private String group;

    @Value("${server.shard-index}")
    private int shardIndex;

    private final String consumerName =
            "stock-server" + UUID.randomUUID();

    @Scheduled(fixedDelay = 100)
    public void consume() {
        String streamKey = prefix + "-" + shardIndex;

        List<MapRecord<String, Object, Object>> records =
                streamRedisTemplate.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty()
                                .count(10)
                                .block(Duration.ofMillis(100)),
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
        if (!"AUTO_CANCEL_CREATED".equals(type)) {
            return;
        }

        Object autoOrderIdObj = value.get("autoOrderId");
        Long autoOrderId = null;
        if (autoOrderIdObj != null) {
            if (autoOrderIdObj instanceof Number) {
                autoOrderId = ((Number) autoOrderIdObj).longValue();
            } else {
                autoOrderId = Long.parseLong(autoOrderIdObj.toString());
            }
        }

        String stockCode = (String) value.get("stockCode");

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip auto cancel for stockCode {}", stockCode);
            return;
        }

        log.info("Processing cancel autoOrderId: {} for stockCode {}", autoOrderId, stockCode);

        autoCancelService.registerAutoCancel(AutoCancelRequestEvent.of(autoOrderId, stockCode));
    }
}
