package arile.toy.stocksystem.stockserver.autocancel.event.subscriber;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
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

    @Value("${server.group}")
    private String stockGroup;

    private final String consumerName =
            "stock-server" + UUID.randomUUID();

    private static final long RETRY_IDLE_MILLIS = 10000;
    private static final int MAX_RETRY_COUNT = 3;

    @Scheduled(fixedDelay = 100)
    public void consume() {
        String streamKey = prefix + "-" + stockGroup;

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

                Map<Object, Object> value = record.getValue();

                String type = (String) value.get("type");
                if (!"AUTO_CANCEL_CREATED".equals(type)) {
                    streamRedisTemplate.opsForStream()
                            .acknowledge(streamKey, group, record.getId());
                    continue;
                }

                String recordId = record.getId().getValue();
                String status = getStatus(recordId);

                if ("DONE".equals(status)) {
                    log.warn("Duplicate DONE skip recordId={}", recordId);
                    streamRedisTemplate.opsForStream()
                            .acknowledge(streamKey, group, record.getId());
                    continue;
                }

                if ("PROCESSING".equals(status)) {
                    log.warn("Still PROCESSING recordId={}", recordId);
                    continue;
                }

                if (!tryStartProcess(recordId)) {
                    continue;
                }

                handle(record);

                markProcessed(recordId);

                streamRedisTemplate.opsForStream()
                        .acknowledge(streamKey, group, record.getId());

            } catch (Exception e) {
                log.error("Failed to process {}", record.getId(), e);
                clearProcessingMark(record.getId().getValue());
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void retry() {
        String streamKey = prefix + "-" + stockGroup;
        retryPending(streamKey);
    }

    private void retryPending(String streamKey) {

        PendingMessages pendingMessages =
                streamRedisTemplate.opsForStream()
                        .pending(streamKey, group, Range.unbounded(), 20);

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        for (PendingMessage msg : pendingMessages) {

            if (msg.getElapsedTimeSinceLastDelivery().toMillis() < RETRY_IDLE_MILLIS) {
                continue;
            }

            List<MapRecord<String, Object, Object>> claimed =
                    streamRedisTemplate.opsForStream().claim(
                            streamKey,
                            group,
                            consumerName,
                            Duration.ofMillis(RETRY_IDLE_MILLIS),
                            msg.getId()
                    );

            for (MapRecord<String, Object, Object> record : claimed) {
                processRetry(streamKey, record);
            }
        }
    }

    private void processRetry(String streamKey, MapRecord<String, Object, Object> record) {

        String recordId = record.getId().getValue();

        try {
            String status = getStatus(recordId);

            if ("DONE".equals(status)) {
                streamRedisTemplate.opsForStream()
                        .acknowledge(streamKey, group, record.getId());
                return;
            }

            if ("PROCESSING".equals(status)) {
                return;
            }

            if (!tryStartProcess(recordId)) {
                return;
            }

            handle(record);

            markProcessed(recordId);

            streamRedisTemplate.opsForStream()
                    .acknowledge(streamKey, group, record.getId());

            clearRetryCount(record);

        } catch (Exception e) {

            int retryCount = getRetryCount(record);

            if (retryCount >= MAX_RETRY_COUNT) {
                moveToDLQ(record);

                streamRedisTemplate.opsForStream()
                        .acknowledge(streamKey, group, record.getId());

                clearRetryCount(record);

            } else {
                increaseRetryCount(record);
            }

            log.error("Retry failed {}", record.getId(), e);
        }
    }

    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

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

    private String retryKey(RecordId id) {
        return "retry:" + id.getValue();
    }

    private int getRetryCount(MapRecord<String, Object, Object> record) {
        Object val = streamRedisTemplate.opsForValue()
                .get(retryKey(record.getId()));
        return val == null ? 0 : Integer.parseInt(val.toString());
    }

    private void increaseRetryCount(MapRecord<String, Object, Object> record) {
        streamRedisTemplate.opsForValue()
                .increment(retryKey(record.getId()));
    }

    private void clearRetryCount(MapRecord<String, Object, Object> record) {
        streamRedisTemplate.delete(retryKey(record.getId()));
    }

    private void moveToDLQ(MapRecord<String, Object, Object> record) {

        Map<String, Object> dlqData = new HashMap<>();
        dlqData.put("original", record.getValue());
        dlqData.put("failedAt", System.currentTimeMillis());
        dlqData.put("recordId", record.getId().getValue());

        streamRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(dlqData)
                        .withStreamKey("auto-cancel-dlq")
        );
    }

    private String processedKey(String recordId) {
        return "processed:autoCancel:" + recordId;
    }

    private boolean tryStartProcess(String recordId) {
        if (recordId == null) return false;

        Boolean success = streamRedisTemplate.opsForValue()
                .setIfAbsent(
                        processedKey(recordId),
                        "PROCESSING",
                        Duration.ofMinutes(5)
                );

        return Boolean.TRUE.equals(success);
    }

    private void markProcessed(String recordId) {
        streamRedisTemplate.opsForValue()
                .set(
                        processedKey(recordId),
                        "DONE",
                        Duration.ofHours(24)
                );
    }

    private String getStatus(String recordId) {
        if (recordId == null) return null;

        Object val = streamRedisTemplate.opsForValue()
                .get(processedKey(recordId));

        return val == null ? null : val.toString();
    }

    private void clearProcessingMark(String recordId) {
        streamRedisTemplate.delete(processedKey(recordId));
    }
}
