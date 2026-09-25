package arile.toy.stocksystem.stockserver.common.stream;

import lombok.extern.slf4j.Slf4j;
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
 * Redis Stream 요청 컨슈머 공통 처리.
 * - 컨슈머 그룹으로 새 메시지를 읽어 처리하고 ack
 * - 처리 상태 키(processed:{namespace}:{recordId})로 중복 처리 방지 (PROCESSING → DONE)
 * - 처리되지 않은 pending 메시지를 일정 시간 후 재시도하고, 한도를 넘으면 DLQ로 이동
 * 하위 클래스는 스트림/이벤트 설정과 handle()만 구현.
 */
@Slf4j
public abstract class AbstractRedisStreamConsumer {

    private static final long RETRY_IDLE_MILLIS = 10000;
    private static final int MAX_RETRY_COUNT = 3;
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE = "DONE";

    protected final RedisTemplate<String, Object> streamRedisTemplate;

    private final String streamKey;
    private final String group;
    private final String eventType;
    private final String keyNamespace;
    private final String dlqStreamKey;

    private final String consumerName = "stock-server" + UUID.randomUUID();

    /**
     * @param streamKey    읽을 스트림 키 (예: order-1)
     * @param group        컨슈머 그룹
     * @param eventType    처리할 이벤트 type 값 (예: ORDER_CREATED). 다른 type은 처리 없이 ack
     * @param keyNamespace 처리 상태·재시도 횟수 Redis 키 구분자 (예: order -> processed:order:, retry:order:)
     * @param dlqStreamKey 재시도 한도 초과 시 옮길 스트림 키 (예: order-dlq)
     */
    protected AbstractRedisStreamConsumer(RedisTemplate<String, Object> streamRedisTemplate,
                                          String streamKey, String group, String eventType,
                                          String keyNamespace, String dlqStreamKey) {
        this.streamRedisTemplate = streamRedisTemplate;
        this.streamKey = streamKey;
        this.group = group;
        this.eventType = eventType;
        this.keyNamespace = keyNamespace;
        this.dlqStreamKey = dlqStreamKey;
    }

    /** 레코드 1건을 실제 도메인 처리로 넘긴다. 예외를 던지면 재시도 대상이 된다. */
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
            String recordId = record.getId().getValue();
            // 이번 호출에서 PROCESSING 선점에 성공했는지 / 처리(DONE 기록)까지 끝났는지
            // → 실패 시 "내가 잡은 미완료 PROCESSING"만 지우기 위함 (DONE을 지우면 재시도에서 중복 처리 발생)
            boolean acquired = false;
            boolean done = false;
            try {

                String type = (String) record.getValue().get("type");
                if (!eventType.equals(type)) {
                    acknowledge(record);
                    continue;
                }

                String status = getStatus(recordId);

                if (DONE.equals(status)) {
                    log.warn("Duplicate DONE skip recordId={}", recordId);
                    acknowledge(record);
                    continue;
                }

                if (PROCESSING.equals(status)) {
                    log.warn("Still PROCESSING recordId={}", recordId);
                    continue;
                }

                if (!tryStartProcess(recordId)) {
                    continue;
                }
                acquired = true;

                handle(record);

                markProcessed(recordId);
                done = true;

                acknowledge(record);
            } catch (Exception e) {
                log.error("Failed to process {}", record.getId(), e);
                if (acquired && !done) {
                    clearProcessingMark(recordId);
                }
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
        boolean acquired = false;
        boolean done = false;

        try {
            String status = getStatus(recordId);

            if (DONE.equals(status)) {
                acknowledge(record);
                return;
            }

            if (PROCESSING.equals(status)) {
                return;
            }

            if (!tryStartProcess(recordId)) {
                return;
            }
            acquired = true;

            handle(record);

            markProcessed(recordId);
            done = true;

            acknowledge(record);

            clearRetryCount(record);

        } catch (Exception e) {

            // 처리는 끝났고 후처리(ack 등)만 실패한 경우: 재시도 횟수 증가·DLQ 이동을 하지 않음
            // → 다음 재시도에서 DONE을 보고 ack만 수행
            if (done) {
                log.error("Retry post-processing failed after DONE {}", record.getId(), e);
                return;
            }

            // 다음 재시도가 PROCESSING에 막혀 TTL(5분)까지 멈추지 않도록 내가 잡은 표시 해제
            if (acquired) {
                clearProcessingMark(recordId);
            }

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
        streamRedisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
    }

    // 스트림마다 레코드 ID(타임스탬프-순번)가 겹칠 수 있으므로 컨슈머별 구분자를 붙임
    private String retryKey(RecordId id) {
        return "retry:" + keyNamespace + ":" + id.getValue();
    }

    private int getRetryCount(MapRecord<String, Object, Object> record) {
        Object val = streamRedisTemplate.opsForValue().get(retryKey(record.getId()));
        return val == null ? 0 : Integer.parseInt(val.toString());
    }

    private void increaseRetryCount(MapRecord<String, Object, Object> record) {
        streamRedisTemplate.opsForValue().increment(retryKey(record.getId()));
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
                        .withStreamKey(dlqStreamKey)
        );
    }

    private String processedKey(String recordId) {
        return "processed:" + keyNamespace + ":" + recordId;
    }

    private boolean tryStartProcess(String recordId) {
        if (recordId == null) return false;

        Boolean success = streamRedisTemplate.opsForValue()
                .setIfAbsent(processedKey(recordId), PROCESSING, Duration.ofMinutes(5));

        return Boolean.TRUE.equals(success);
    }

    private void markProcessed(String recordId) {
        streamRedisTemplate.opsForValue()
                .set(processedKey(recordId), DONE, Duration.ofHours(24));
    }

    private String getStatus(String recordId) {
        if (recordId == null) return null;

        Object val = streamRedisTemplate.opsForValue().get(processedKey(recordId));

        return val == null ? null : val.toString();
    }

    private void clearProcessingMark(String recordId) {
        streamRedisTemplate.delete(processedKey(recordId));
    }
}
