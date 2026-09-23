package arile.toy.stocksystem.accountserver.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Redis Stream 컨슈머 공통 로직.
 * - consume: 신규 레코드 소비 (타입 필터, 중복 처리 방지, 처리 후 ack)
 * - retry: 일정 시간 이상 pending된 레코드를 claim하여 재처리, 최대 재시도 초과 시 DLQ 이동
 * 하위 클래스는 이벤트 타입, 키 prefix, DLQ 스트림, 실제 처리(handle)만 구현
 */
public abstract class AbstractRedisStreamConsumer {

    private static final long RETRY_IDLE_MILLIS = 10000;
    private static final int MAX_RETRY_COUNT = 3;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final RedisTemplate<String, Object> streamRedisTemplate;
    private final String streamKey;
    private final String group;

    private final String consumerName =
            "account-server" + UUID.randomUUID();

    protected AbstractRedisStreamConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            String streamKey,
            String group
    ) {
        this.streamRedisTemplate = streamRedisTemplate;
        this.streamKey = streamKey;
        this.group = group;
    }

    /** 처리 대상 이벤트 타입 (레코드의 "type" 필드 값) */
    protected abstract String eventType();

    /** 중복 처리 방지용 상태 키 prefix */
    protected abstract String processedKeyPrefix();

    /** 최대 재시도 초과 시 이동할 DLQ 스트림 키 */
    protected abstract String dlqStreamKey();

    /** 실제 이벤트 처리. 예외를 던지면 재시도 대상이 된다. */
    protected abstract void handle(MapRecord<String, Object, Object> record);

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

                String type = (String) record.getValue().get("type");
                if (!eventType().equals(type)) {
                    acknowledge(record);
                    continue;
                }

                String recordId = record.getId().getValue();
                String status = getStatus(recordId);

                if ("DONE".equals(status)) {
                    log.warn("Duplicate DONE skip recordId={}", recordId);
                    acknowledge(record);
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

                acknowledge(record);
            } catch (Exception e) {
                log.error("Failed to process {}", record.getId(), e);
                clearProcessingMark(record.getId().getValue());
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void retry() {

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
                processRetry(record);
            }
        }
    }

    private void processRetry(MapRecord<String, Object, Object> record) {

        String recordId = record.getId().getValue();

        try {
            String status = getStatus(recordId);

            if ("DONE".equals(status)) {
                acknowledge(record);
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

            acknowledge(record);

            clearRetryCount(record);

        } catch (Exception e) {

            int retryCount = getRetryCount(record);

            if (retryCount >= MAX_RETRY_COUNT) {
                moveToDLQ(record);

                acknowledge(record);

                clearRetryCount(record);

            } else {
                increaseRetryCount(record);
            }

            log.error("Retry failed {}", record.getId(), e);
        }
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        streamRedisTemplate.opsForStream()
                .acknowledge(streamKey, group, record.getId());
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
                        .withStreamKey(dlqStreamKey())
        );
    }

    private String processedKey(String recordId) {
        return processedKeyPrefix() + recordId;
    }

    private boolean tryStartProcess(String recordId) {
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
        Object val = streamRedisTemplate.opsForValue()
                .get(processedKey(recordId));

        return val == null ? null : val.toString();
    }

    private void clearProcessingMark(String recordId) {
        streamRedisTemplate.delete(processedKey(recordId));
    }
}
