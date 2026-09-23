package arile.toy.stocksystem.accountserver.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AbstractRedisStreamConsumerTest {

    private static final String STREAM_KEY = "test-events";
    private static final String GROUP = "test-group";
    private static final String EVENT_TYPE = "TEST_EVENT";
    private static final RecordId RECORD_ID = RecordId.of("1-0");
    private static final String PROCESSED_KEY = "processed:test:1-0";
    private static final String RETRY_KEY = "retry:1-0";

    /** handle() 결과를 테스트에서 조절하는 하위 클래스 */
    static class TestStreamConsumer extends AbstractRedisStreamConsumer {

        final List<MapRecord<String, Object, Object>> handled = new ArrayList<>();
        RuntimeException failure;

        TestStreamConsumer(RedisTemplate<String, Object> template) {
            super(template, STREAM_KEY, GROUP);
        }

        @Override
        protected String eventType() {
            return EVENT_TYPE;
        }

        @Override
        protected String processedKeyPrefix() {
            return "processed:test:";
        }

        @Override
        protected String dlqStreamKey() {
            return "test-dlq";
        }

        @Override
        protected void handle(MapRecord<String, Object, Object> record) {
            if (failure != null) {
                throw failure;
            }
            handled.add(record);
        }
    }

    private RedisTemplate<String, Object> streamRedisTemplate;
    private StreamOperations<String, Object, Object> streamOps;
    private ValueOperations<String, Object> valueOps;

    private TestStreamConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        streamRedisTemplate = mock(RedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        valueOps = mock(ValueOperations.class);

        lenient().doReturn(streamOps).when(streamRedisTemplate).opsForStream();
        lenient().doReturn(valueOps).when(streamRedisTemplate).opsForValue();

        consumer = new TestStreamConsumer(streamRedisTemplate);
    }

    private static MapRecord<String, Object, Object> record(String type) {
        return StreamRecords.newRecord()
                .in(STREAM_KEY)
                .withId(RECORD_ID)
                .ofMap(Map.<Object, Object>of("type", type, "data", "value"));
    }

    private void verifyAcked() {
        verify(streamOps).acknowledge(STREAM_KEY, GROUP, RECORD_ID);
    }

    private void verifyNotAcked() {
        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
    }

    private void givenStatus(Object status) {
        given(valueOps.get(PROCESSED_KEY)).willReturn(status);
    }

    private void givenStartProcess(Boolean acquired) {
        given(valueOps.setIfAbsent(PROCESSED_KEY, "PROCESSING", Duration.ofMinutes(5)))
                .willReturn(acquired);
    }

    // ===================== consume =====================

    @Nested
    @DisplayName("consume")
    class Consume {

        private void givenRead(List<MapRecord<String, Object, Object>> records) {
            doReturn(records).when(streamOps).read(
                    any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class));
        }

        @Test
        @DisplayName("읽은 레코드가 null이면 아무것도 하지 않는다")
        void whenRecordsNull_doesNothing() {
            givenRead(null);

            consumer.consume();

            assertThat(consumer.handled).isEmpty();
            verifyNotAcked();
        }

        @Test
        @DisplayName("읽은 레코드가 없으면 아무것도 하지 않는다")
        void whenRecordsEmpty_doesNothing() {
            givenRead(List.of());

            consumer.consume();

            assertThat(consumer.handled).isEmpty();
            verifyNotAcked();
        }

        @Test
        @DisplayName("대상 타입이 아닌 이벤트는 처리하지 않고 ack한다")
        void whenOtherType_acksWithoutHandling() {
            givenRead(List.of(record("OTHER_EVENT")));

            consumer.consume();

            assertThat(consumer.handled).isEmpty();
            verifyAcked();
        }

        @Test
        @DisplayName("이미 DONE인 레코드는 중복 처리하지 않고 ack한다")
        void whenAlreadyDone_acksWithoutHandling() {
            givenRead(List.of(record(EVENT_TYPE)));
            givenStatus("DONE");

            consumer.consume();

            assertThat(consumer.handled).isEmpty();
            verifyAcked();
        }

        @Test
        @DisplayName("PROCESSING 중인 레코드는 건너뛰고 ack하지 않는다")
        void whenProcessing_skips() {
            givenRead(List.of(record(EVENT_TYPE)));
            givenStatus("PROCESSING");

            consumer.consume();

            assertThat(consumer.handled).isEmpty();
            verifyNotAcked();
        }

        @Test
        @DisplayName("다른 컨슈머가 선점하면 건너뛴다")
        void whenStartProcessFails_skips() {
            givenRead(List.of(record(EVENT_TYPE)));
            givenStatus(null);
            givenStartProcess(false);

            consumer.consume();

            assertThat(consumer.handled).isEmpty();
            verifyNotAcked();
        }

        @Test
        @DisplayName("처리에 성공하면 DONE으로 마킹한 뒤 ack한다")
        void success_marksDoneAndAcks() {
            givenRead(List.of(record(EVENT_TYPE)));
            givenStatus(null);
            givenStartProcess(true);

            consumer.consume();

            assertThat(consumer.handled).containsExactly(record(EVENT_TYPE));
            verify(valueOps).set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            verifyAcked();
        }

        @Test
        @DisplayName("처리 중 예외가 나면 PROCESSING 마크를 지우고 ack하지 않는다 (pending으로 남김)")
        void whenHandleFails_clearsProcessingMark() {
            givenRead(List.of(record(EVENT_TYPE)));
            givenStatus(null);
            givenStartProcess(true);
            consumer.failure = new RuntimeException("fail");

            consumer.consume();

            verify(streamRedisTemplate).delete(PROCESSED_KEY);
            verifyNotAcked();
        }
    }

    // ===================== retry =====================

    @Nested
    @DisplayName("retry")
    class Retry {

        private void givenPending(PendingMessages pendingMessages) {
            given(streamOps.pending(eq(STREAM_KEY), eq(GROUP), any(Range.class), eq(20L)))
                    .willReturn(pendingMessages);
        }

        private PendingMessages pendingWithIdle(Duration idle) {
            PendingMessage message = new PendingMessage(
                    RECORD_ID, Consumer.from(GROUP, "other-consumer"), idle, 1L);
            return new PendingMessages(GROUP, List.of(message));
        }

        private void givenClaimed() {
            givenPending(pendingWithIdle(Duration.ofSeconds(30)));
            doReturn(List.of(record(EVENT_TYPE))).when(streamOps).claim(
                    eq(STREAM_KEY), eq(GROUP), anyString(), eq(Duration.ofMillis(10_000)), eq(RECORD_ID));
        }

        private void givenHandleFails() {
            givenStatus(null);
            givenStartProcess(true);
            consumer.failure = new RuntimeException("fail");
        }

        private void verifyNotClaimed() {
            verify(streamOps, never()).claim(
                    anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @Test
        @DisplayName("pending이 null이면 아무것도 하지 않는다")
        void whenPendingNull_doesNothing() {
            givenPending(null);

            consumer.retry();

            verifyNotClaimed();
        }

        @Test
        @DisplayName("pending이 비어 있으면 아무것도 하지 않는다")
        void whenPendingEmpty_doesNothing() {
            givenPending(new PendingMessages(GROUP, List.of()));

            consumer.retry();

            verifyNotClaimed();
        }

        @Test
        @DisplayName("마지막 전달 후 대기 시간이 기준 미만이면 claim하지 않는다")
        void whenNotIdleEnough_skips() {
            givenPending(pendingWithIdle(Duration.ofSeconds(1)));

            consumer.retry();

            verifyNotClaimed();
        }

        @Test
        @DisplayName("claim한 레코드가 이미 DONE이면 ack만 한다")
        void whenClaimedDone_acks() {
            givenClaimed();
            givenStatus("DONE");

            consumer.retry();

            assertThat(consumer.handled).isEmpty();
            verifyAcked();
        }

        @Test
        @DisplayName("claim한 레코드가 PROCESSING이면 건너뛴다")
        void whenClaimedProcessing_skips() {
            givenClaimed();
            givenStatus("PROCESSING");

            consumer.retry();

            assertThat(consumer.handled).isEmpty();
            verifyNotAcked();
        }

        @Test
        @DisplayName("claim한 레코드의 선점에 실패하면 건너뛴다")
        void whenClaimedStartProcessFails_skips() {
            givenClaimed();
            givenStatus(null);
            givenStartProcess(false);

            consumer.retry();

            assertThat(consumer.handled).isEmpty();
            verifyNotAcked();
        }

        @Test
        @DisplayName("재처리에 성공하면 DONE 마킹, ack 후 retry 카운트를 삭제한다")
        void whenRetrySucceeds_acksAndClearsRetryCount() {
            givenClaimed();
            givenStatus(null);
            givenStartProcess(true);

            consumer.retry();

            assertThat(consumer.handled).hasSize(1);
            verify(valueOps).set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            verifyAcked();
            verify(streamRedisTemplate).delete(RETRY_KEY);
        }

        @Test
        @DisplayName("재처리 실패 시 retry 카운트가 없으면(0회) 카운트를 증가시킨다")
        void whenRetryFailsFirstTime_increasesCount() {
            givenClaimed();
            givenHandleFails();
            given(valueOps.get(RETRY_KEY)).willReturn(null);

            consumer.retry();

            verify(valueOps).increment(RETRY_KEY);
            verifyNotAcked();
        }

        @Test
        @DisplayName("재처리 실패 시 retry 카운트가 최대치 미만이면 카운트를 증가시킨다")
        void whenRetryFailsUnderMax_increasesCount() {
            givenClaimed();
            givenHandleFails();
            given(valueOps.get(RETRY_KEY)).willReturn("2");

            consumer.retry();

            verify(valueOps).increment(RETRY_KEY);
            verify(streamOps, never()).add(any(MapRecord.class));
            verifyNotAcked();
        }

        @Test
        @DisplayName("재처리 실패 시 retry 카운트가 최대치에 도달하면 DLQ로 옮기고 ack한다")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void whenRetryFailsAtMax_movesToDlq() {
            givenClaimed();
            givenHandleFails();
            given(valueOps.get(RETRY_KEY)).willReturn("3");

            consumer.retry();

            ArgumentCaptor<MapRecord> dlqCaptor = ArgumentCaptor.forClass(MapRecord.class);
            verify(streamOps).add(dlqCaptor.capture());
            MapRecord dlqRecord = dlqCaptor.getValue();
            assertThat(dlqRecord.getStream()).isEqualTo("test-dlq");
            assertThat(((Map<Object, Object>) dlqRecord.getValue()))
                    .containsEntry("recordId", "1-0")
                    .containsEntry("original", Map.of("type", EVENT_TYPE, "data", "value"))
                    .containsKey("failedAt");

            verifyAcked();
            verify(streamRedisTemplate).delete(RETRY_KEY);
            verify(valueOps, never()).increment(anyString());
        }
    }
}
