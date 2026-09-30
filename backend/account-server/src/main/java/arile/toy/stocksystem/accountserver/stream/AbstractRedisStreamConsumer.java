package arile.toy.stocksystem.accountserver.stream;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Redis Stream 컨슈머 공통 로직.
 * - consume: 신규 레코드 소비 (타입 필터, 중복 처리 방지, 처리 후 ack)
 * - retry: 일정 시간 이상 pending된 레코드를 claim하여 재처리, 최대 재시도 초과 시 DLQ 이동
 * 하위 클래스는 이벤트 타입, 키 prefix, DLQ 스트림, 실제 처리(handle)만 구현
 */
public abstract class AbstractRedisStreamConsumer {

    private static final long RETRY_IDLE_MILLIS = 10000;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 10;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final RedisTemplate<String, Object> streamRedisTemplate;
    private final String streamKey;
    private final String group;

    private final String consumerName =
            "account-server" + UUID.randomUUID();

    private MeterRegistry meterRegistry = Metrics.globalRegistry;

    private ExecutorService workers;

    @Autowired
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

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

    protected Object partitionKey(MapRecord<String, Object, Object> record) {
        return null;
    }

    protected int workerCount() {
        return 1;
    }

    @Scheduled(fixedDelay = 100)
    public void consume() {
        while (consumeBatch() == BATCH_SIZE) {
        }
    }

    private int consumeBatch() {

        List<MapRecord<String, Object, Object>> records =
                streamRedisTemplate.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty()
                                .count(BATCH_SIZE)
                                .block(Duration.ofMillis(100)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) {
            return 0;
        }

        meterRegistry.summary("stream.consume.batch.size", "stream", streamKey)
                .record(records.size());

        if (workerCount() <= 1) {
            records.forEach(this::processRecord);
        } else {
            processInParallel(records);
        }
        return records.size();
    }

    private void processInParallel(List<MapRecord<String, Object, Object>> records) {
        Map<Object, List<MapRecord<String, Object, Object>>> groups = new LinkedHashMap<>();
        for (MapRecord<String, Object, Object> record : records) {
            Object key = partitionKey(record);
            groups.computeIfAbsent(key == null ? "" : key, k -> new ArrayList<>()).add(record);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (List<MapRecord<String, Object, Object>> group : groups.values()) {
            futures.add(CompletableFuture.runAsync(() -> group.forEach(this::processRecord), workers()));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private synchronized ExecutorService workers() {
        if (workers == null) {
            workers = Executors.newFixedThreadPool(workerCount());
        }
        return workers;
    }

    @PreDestroy
    public synchronized void shutdownWorkers() throws InterruptedException {
        if (workers != null) {
            workers.shutdown();
            workers.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        String recordId = record.getId().getValue();
        boolean acquired = false;
        boolean done = false;
        try {

            String type = (String) record.getValue().get("type");
            if (!eventType().equals(type)) {
                acknowledge(record);
                return;
            }

            String status = getStatus(recordId);

            if ("DONE".equals(status)) {
                log.warn("Duplicate DONE skip recordId={}", recordId);
                acknowledge(record);
                return;
            }

            if ("PROCESSING".equals(status)) {
                log.warn("Still PROCESSING recordId={}", recordId);
                return;
            }

            if (!tryStartProcess(recordId)) {
                return;
            }
            acquired = true;

            recordWaitTime(record);
            Timer.Sample sample = Timer.start(meterRegistry);
            handle(record);
            sample.stop(handleTimer());

            markProcessed(recordId);
            done = true;

            acknowledge(record);
        } catch (Exception e) {
            meterRegistry.counter("stream.consume.errors", "stream", streamKey).increment();
            log.error("Failed to process {}", record.getId(), e);
            if (acquired && !done) {
                clearProcessingMark(recordId);
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

    // 레코드 ID 앞부분은 XADD 시각(ms) → 발행부터 처리 시작까지 스트림에서 기다린 시간
    private void recordWaitTime(MapRecord<String, Object, Object> record) {
        long waitMillis = System.currentTimeMillis() - record.getId().getTimestamp();
        Timer.builder("stream.consume.wait")
                .tag("stream", streamKey)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(waitMillis, 0)));
    }

    private Timer handleTimer() {
        return Timer.builder("stream.consume.handle")
                .tag("stream", streamKey)
                .publishPercentileHistogram()
                .register(meterRegistry);
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
