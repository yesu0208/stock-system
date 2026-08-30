package arile.toy.stocksystem.accountserver.trade.event.subscriber;

import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.trade.service.TradeExecutionApplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RedisTradeExecutedEventConsumer {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final TradeExecutionApplyService tradeExecutionApplyService;
    private final ObjectMapper objectMapper;

    @Value("${redis.streams.trade-executed.key}")
    private String streamKey;

    @Value("${redis.streams.trade-executed.consumer-group}")
    private String group;

    private final String consumerName =
            "account-server" + UUID.randomUUID();

    private static final long RETRY_IDLE_MILLIS = 10000;
    private static final int MAX_RETRY_COUNT = 3;

    @Scheduled(fixedDelay = 100)
    public void consume() {

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
                if (!"TRADE_EXECUTED".equals(type)) {
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

        String payload = (String) value.get("payload");

        try {
            TradeExecutedEvent event = objectMapper.readValue(payload, TradeExecutedEvent.class);
            tradeExecutionApplyService.apply(event);
        } catch (Exception e) {
            log.error("Failed to parse/apply TradeExecutedEvent. payload={}", payload, e);
            throw new IllegalStateException(e);
        }
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
                        .withStreamKey("trade-executed-dlq")
        );
    }

    private String processedKey(String recordId) {
        return "processed:tradeExecuted:" + recordId;
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
