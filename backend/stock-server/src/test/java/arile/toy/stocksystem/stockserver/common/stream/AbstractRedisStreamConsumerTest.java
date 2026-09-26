package arile.toy.stocksystem.stockserver.common.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] Redis Stream 컨슈머 공통 처리·재시도·DLQ 테스트")
@ExtendWith(MockitoExtension.class)
class AbstractRedisStreamConsumerTest {

    private static final String STREAM_KEY = "test-1";
    private static final String GROUP = "test-group";
    private static final String EVENT_TYPE = "TEST_CREATED";
    private static final String DLQ_KEY = "test-dlq";
    private static final RecordId RECORD_ID = RecordId.of("1-0");
    private static final String PROCESSED_KEY = "processed:test:1-0";
    private static final String RETRY_KEY = "retry:test:1-0";

    /** 테스트용 컨슈머: handle()을 mock에 위임 */
    interface RecordHandler {
        void handle(MapRecord<String, Object, Object> record);
    }

    static class TestConsumer extends AbstractRedisStreamConsumer {
        private final RecordHandler handler;

        TestConsumer(RedisTemplate<String, Object> template, RecordHandler handler) {
            super(template, STREAM_KEY, GROUP, EVENT_TYPE, "test", DLQ_KEY);
            this.handler = handler;
        }

        @Override
        protected void handle(MapRecord<String, Object, Object> record) {
            handler.handle(record);
        }
    }

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private RecordHandler handler;
    @Mock private StreamOperations<String, Object, Object> streamOps;
    @Mock private ValueOperations<String, Object> valueOps;

    private TestConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new TestConsumer(streamRedisTemplate, handler);
        lenient().doReturn(streamOps).when(streamRedisTemplate).opsForStream();
        lenient().doReturn(valueOps).when(streamRedisTemplate).opsForValue();
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @DisplayName("읽은 레코드가 없으면 아무것도 하지 않는다")
        @Test
        void givenNoRecords_whenConsuming_thenDoesNothing() {
            givenRead(List.of());

            sut.consume();

            then(handler).shouldHaveNoInteractions();
            then(valueOps).shouldHaveNoInteractions();
        }

        @DisplayName("읽은 결과가 null이면 아무것도 하지 않는다")
        @Test
        void givenNullRecords_whenConsuming_thenDoesNothing() {
            givenRead(null);

            sut.consume();

            then(handler).shouldHaveNoInteractions();
            then(valueOps).shouldHaveNoInteractions();
        }

        @DisplayName("대상 이벤트 타입이 아닌 레코드는 처리 없이 ack한다")
        @Test
        void givenOtherType_whenConsuming_thenAcksWithoutProcessing() {
            Map<Object, Object> value = value();
            value.put("type", "SOMETHING_ELSE");
            givenRead(List.of(record("1-0", value)));

            sut.consume();

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(handler).shouldHaveNoInteractions();
        }

        @DisplayName("이미 DONE인 레코드는 중복 처리하지 않고 ack한다")
        @Test
        void givenDone_whenConsuming_thenSkipsAndAcks() {
            givenRead(List.of(record()));
            given(valueOps.get(PROCESSED_KEY)).willReturn("DONE");

            sut.consume();

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(handler).shouldHaveNoInteractions();
        }

        @DisplayName("다른 곳에서 PROCESSING 중인 레코드는 건너뛰고 ack하지 않는다")
        @Test
        void givenProcessing_whenConsuming_thenSkipsWithoutAck() {
            givenRead(List.of(record()));
            given(valueOps.get(PROCESSED_KEY)).willReturn("PROCESSING");

            sut.consume();

            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
            then(handler).shouldHaveNoInteractions();
        }

        @DisplayName("PROCESSING 선점에 실패하면 처리하지 않는다")
        @Test
        void givenAcquireFails_whenConsuming_thenSkips() {
            givenRead(List.of(record()));
            given(valueOps.setIfAbsent(PROCESSED_KEY, "PROCESSING", Duration.ofMinutes(5))).willReturn(false);

            sut.consume();

            then(handler).shouldHaveNoInteractions();
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("정상 레코드는 handle 후 DONE 기록하고 ack한다")
        @Test
        void givenValidRecord_whenConsuming_thenHandlesMarksDoneAndAcks() {
            var record = record();
            givenRead(List.of(record));
            givenAcquire();

            sut.consume();

            then(handler).should().handle(record);
            then(valueOps).should().set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
        }

        @DisplayName("handle이 실패하면 PROCESSING 표시를 지우고 ack하지 않아 재시도 대상으로 남긴다")
        @Test
        void givenHandleFails_whenConsuming_thenClearsProcessingAndDoesNotAck() {
            givenRead(List.of(record()));
            givenAcquire();
            willThrow(new IllegalStateException("fail")).given(handler).handle(any());

            sut.consume();

            then(streamRedisTemplate).should().delete(PROCESSED_KEY);
            then(valueOps).should(never()).set(anyString(), eq("DONE"), any(Duration.class));
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("DONE 기록 후 ack만 실패하면 DONE 표시를 지우지 않는다 (중복 처리 방지)")
        @Test
        void givenAckFailsAfterDone_whenConsuming_thenKeepsDoneMark() {
            givenRead(List.of(record()));
            givenAcquire();
            given(streamOps.acknowledge(STREAM_KEY, GROUP, RECORD_ID)).willThrow(new IllegalStateException("redis"));

            sut.consume();

            then(valueOps).should().set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            then(streamRedisTemplate).should(never()).delete(anyString());
        }

        @DisplayName("상태 조회 자체가 실패하면 선점한 적이 없으므로 표시를 지우지 않는다")
        @Test
        void givenStatusLookupFails_whenConsuming_thenDoesNotClearMark() {
            givenRead(List.of(record()));
            given(valueOps.get(PROCESSED_KEY)).willThrow(new IllegalStateException("redis"));

            sut.consume();

            then(streamRedisTemplate).should(never()).delete(anyString());
            then(handler).shouldHaveNoInteractions();
        }

        @DisplayName("한 레코드가 실패해도 다음 레코드는 계속 처리한다")
        @Test
        void givenFirstRecordFails_whenConsuming_thenContinuesWithNext() {
            givenRead(List.of(record("1-0", value()), record("2-0", value())));
            given(valueOps.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).willReturn(true);
            willThrow(new IllegalStateException("fail")).willDoNothing().given(handler).handle(any());

            sut.consume();

            then(handler).should(times(2)).handle(any());
            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RecordId.of("2-0"));
        }
    }

    @Nested
    @DisplayName("retry")
    class Retry {

        @DisplayName("pending 메시지가 없으면 아무것도 하지 않는다")
        @Test
        void givenNoPending_whenRetrying_thenDoesNothing() {
            givenPending(List.of());

            sut.retry();

            then(streamOps).should(never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @DisplayName("pending 결과가 null이면 아무것도 하지 않는다")
        @Test
        void givenNullPending_whenRetrying_thenDoesNothing() {
            doReturn(null).when(streamOps).pending(eq(STREAM_KEY), eq(GROUP), any(Range.class), eq(20L));

            sut.retry();

            then(streamOps).should(never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @DisplayName("마지막 전달 후 10초가 지나지 않은 메시지는 가져오지 않는다")
        @Test
        void givenRecentlyDelivered_whenRetrying_thenDoesNotClaim() {
            givenPending(List.of(pendingMessage(Duration.ofSeconds(5))));

            sut.retry();

            then(streamOps).should(never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @DisplayName("재처리에 성공하면 handle 후 ack하고 재시도 횟수를 지운다")
        @Test
        void givenClaimedRecord_whenRetrySucceeds_thenAcksAndClearsRetryCount() {
            givenClaimed(record());
            givenAcquire();

            sut.retry();

            then(handler).should().handle(any());
            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(streamRedisTemplate).should().delete(RETRY_KEY);
        }

        @DisplayName("재처리 시 이미 DONE이면 ack만 한다")
        @Test
        void givenDone_whenRetrying_thenOnlyAcks() {
            givenClaimed(record());
            given(valueOps.get(PROCESSED_KEY)).willReturn("DONE");

            sut.retry();

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(handler).shouldHaveNoInteractions();
        }

        @DisplayName("재처리 시 PROCESSING 중이면 건너뛴다")
        @Test
        void givenProcessing_whenRetrying_thenSkips() {
            givenClaimed(record());
            given(valueOps.get(PROCESSED_KEY)).willReturn("PROCESSING");

            sut.retry();

            then(handler).shouldHaveNoInteractions();
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("재처리 시 PROCESSING 선점에 실패하면 처리·ack 없이 넘어간다")
        @Test
        void givenAcquireFails_whenRetrying_thenSkips() {
            givenClaimed(record());
            given(valueOps.setIfAbsent(PROCESSED_KEY, "PROCESSING", Duration.ofMinutes(5))).willReturn(false);

            sut.retry();

            then(handler).shouldHaveNoInteractions();
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("재처리 중 선점 전에 실패하면 표시를 지우지 않고 재시도 횟수만 올린다")
        @Test
        void givenFailureBeforeAcquire_whenRetrying_thenOnlyIncreasesCount() {
            givenClaimed(record());
            given(valueOps.get(PROCESSED_KEY)).willThrow(new IllegalStateException("redis"));
            given(valueOps.get(RETRY_KEY)).willReturn(null);

            sut.retry();

            then(handler).shouldHaveNoInteractions();
            then(streamRedisTemplate).should(never()).delete(anyString());
            then(valueOps).should().increment(RETRY_KEY);
            then(streamOps).should(never()).add(any(MapRecord.class));
        }

        @DisplayName("재처리 실패 횟수가 한도 미만이면 PROCESSING 표시를 지우고 재시도 횟수를 올린다")
        @Test
        void givenFailureUnderLimit_whenRetrying_thenClearsMarkAndIncreasesCount() {
            givenClaimed(record());
            givenAcquire();
            willThrow(new IllegalStateException("fail")).given(handler).handle(any());
            given(valueOps.get(PROCESSED_KEY)).willReturn(null);
            given(valueOps.get(RETRY_KEY)).willReturn("1");

            sut.retry();

            then(streamRedisTemplate).should().delete(PROCESSED_KEY);
            then(valueOps).should().increment(RETRY_KEY);
            then(streamOps).should(never()).add(any(MapRecord.class));
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @DisplayName("재처리 실패 횟수가 한도에 도달하면 DLQ로 옮기고 ack한 뒤 재시도 횟수를 지운다")
        @Test
        void givenFailureAtLimit_whenRetrying_thenMovesToDlq() {
            Map<Object, Object> value = value();
            givenClaimed(record("1-0", value));
            givenAcquire();
            willThrow(new IllegalStateException("fail")).given(handler).handle(any());
            given(valueOps.get(PROCESSED_KEY)).willReturn(null);
            given(valueOps.get(RETRY_KEY)).willReturn("3");

            sut.retry();

            ArgumentCaptor<MapRecord> dlqCaptor = ArgumentCaptor.forClass(MapRecord.class);
            then(streamOps).should().add(dlqCaptor.capture());
            MapRecord dlqRecord = dlqCaptor.getValue();
            assertThat(dlqRecord.getStream()).isEqualTo(DLQ_KEY);
            Map<String, Object> dlqValue = (Map<String, Object>) dlqRecord.getValue();
            assertThat(dlqValue).containsEntry("original", value)
                    .containsEntry("recordId", "1-0")
                    .containsKey("failedAt");

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(streamRedisTemplate).should().delete(PROCESSED_KEY);
            then(streamRedisTemplate).should().delete(RETRY_KEY);
            then(valueOps).should(never()).increment(anyString());
        }

        @DisplayName("재처리 중 DONE 기록 후 ack만 실패하면 재시도 횟수를 올리거나 DLQ로 옮기지 않는다")
        @Test
        void givenAckFailsAfterDone_whenRetrying_thenNoCountNoDlq() {
            givenClaimed(record());
            givenAcquire();
            given(streamOps.acknowledge(STREAM_KEY, GROUP, RECORD_ID)).willThrow(new IllegalStateException("redis"));

            sut.retry();

            then(valueOps).should().set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            then(streamRedisTemplate).should(never()).delete(anyString());
            then(valueOps).should(never()).increment(anyString());
            then(streamOps).should(never()).add(any(MapRecord.class));
        }
    }

    // ===== helpers =====

    private Map<Object, Object> value() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", EVENT_TYPE);
        value.put("payload", "data");
        return value;
    }

    private MapRecord<String, Object, Object> record() {
        return record("1-0", value());
    }

    private MapRecord<String, Object, Object> record(String id, Map<Object, Object> value) {
        return StreamRecords.newRecord()
                .in(STREAM_KEY)
                .withId(RecordId.of(id))
                .ofMap(value);
    }

    private void givenRead(List<MapRecord<String, Object, Object>> records) {
        doReturn(records).when(streamOps)
                .read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class));
    }

    private void givenAcquire() {
        given(valueOps.setIfAbsent(PROCESSED_KEY, "PROCESSING", Duration.ofMinutes(5))).willReturn(true);
    }

    private PendingMessage pendingMessage(Duration idle) {
        return new PendingMessage(RECORD_ID, Consumer.from(GROUP, "other"), idle, 1L);
    }

    private void givenPending(List<PendingMessage> messages) {
        given(streamOps.pending(eq(STREAM_KEY), eq(GROUP), any(Range.class), eq(20L)))
                .willReturn(new PendingMessages(GROUP, messages));
    }

    private void givenClaimed(MapRecord<String, Object, Object> record) {
        givenPending(List.of(pendingMessage(Duration.ofSeconds(11))));
        doReturn(List.of(record)).when(streamOps)
                .claim(eq(STREAM_KEY), eq(GROUP), anyString(), eq(Duration.ofMillis(10_000)), eq(RECORD_ID));
    }
}
