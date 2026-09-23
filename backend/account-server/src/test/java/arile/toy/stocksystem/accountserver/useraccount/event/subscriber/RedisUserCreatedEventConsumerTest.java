package arile.toy.stocksystem.accountserver.useraccount.event.subscriber;

import arile.toy.stocksystem.accountserver.useraccount.service.UserAccountService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisUserCreatedEventConsumerTest {

    private static final String STREAM_KEY = "user-events";
    private static final String GROUP = "account-group";
    private static final String USERNAME = "user1";
    private static final RecordId RECORD_ID = RecordId.of("1-0");
    private static final String PROCESSED_KEY = "processed:user-create:1-0";
    private static final String RETRY_KEY = "retry:1-0";

    private RedisTemplate<String, Object> streamRedisTemplate;
    private StreamOperations<String, Object, Object> streamOps;
    private ValueOperations<String, Object> valueOps;
    private UserAccountService userAccountService;

    private RedisUserCreatedEventConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        streamRedisTemplate = mock(RedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        valueOps = mock(ValueOperations.class);
        userAccountService = mock(UserAccountService.class);

        lenient().doReturn(streamOps).when(streamRedisTemplate).opsForStream();
        lenient().doReturn(valueOps).when(streamRedisTemplate).opsForValue();

        consumer = new RedisUserCreatedEventConsumer(
                streamRedisTemplate, userAccountService, STREAM_KEY, GROUP);
    }

    private static MapRecord<String, Object, Object> record(String type) {
        return StreamRecords.newRecord()
                .in(STREAM_KEY)
                .withId(RECORD_ID)
                .ofMap(Map.<Object, Object>of("type", type, "username", USERNAME));
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

            verifyNoInteractions(userAccountService);
            verifyNotAcked();
        }

        @Test
        @DisplayName("읽은 레코드가 없으면 아무것도 하지 않는다")
        void whenRecordsEmpty_doesNothing() {
            givenRead(List.of());

            consumer.consume();

            verifyNoInteractions(userAccountService);
            verifyNotAcked();
        }

        @Test
        @DisplayName("USER_CREATED가 아닌 이벤트는 처리하지 않고 ack한다")
        void whenOtherType_acksWithoutHandling() {
            givenRead(List.of(record("USER_DELETED")));

            consumer.consume();

            verifyNoInteractions(userAccountService);
            verifyAcked();
        }

        @Test
        @DisplayName("이미 DONE인 레코드는 중복 처리하지 않고 ack한다")
        void whenAlreadyDone_acksWithoutHandling() {
            givenRead(List.of(record("USER_CREATED")));
            givenStatus("DONE");

            consumer.consume();

            verifyNoInteractions(userAccountService);
            verifyAcked();
        }

        @Test
        @DisplayName("PROCESSING 중인 레코드는 건너뛰고 ack하지 않는다")
        void whenProcessing_skips() {
            givenRead(List.of(record("USER_CREATED")));
            givenStatus("PROCESSING");

            consumer.consume();

            verifyNoInteractions(userAccountService);
            verifyNotAcked();
        }

        @Test
        @DisplayName("다른 컨슈머가 선점하면 건너뛴다")
        void whenStartProcessFails_skips() {
            givenRead(List.of(record("USER_CREATED")));
            givenStatus(null);
            givenStartProcess(false);

            consumer.consume();

            verifyNoInteractions(userAccountService);
            verifyNotAcked();
        }

        @Test
        @DisplayName("계좌를 생성하고 DONE으로 마킹한 뒤 ack한다")
        void success_createsAccountAndAcks() {
            givenRead(List.of(record("USER_CREATED")));
            givenStatus(null);
            givenStartProcess(true);

            consumer.consume();

            verify(userAccountService).createAccountIfAbsent(USERNAME);
            verify(valueOps).set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            verifyAcked();
        }

        @Test
        @DisplayName("처리 중 예외가 나면 PROCESSING 마크를 지우고 ack하지 않는다 (pending으로 남김)")
        void whenHandleFails_clearsProcessingMark() {
            givenRead(List.of(record("USER_CREATED")));
            givenStatus(null);
            givenStartProcess(true);
            willThrow(new RuntimeException("db down"))
                    .given(userAccountService).createAccountIfAbsent(USERNAME);

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
            doReturn(List.of(record("USER_CREATED"))).when(streamOps).claim(
                    eq(STREAM_KEY), eq(GROUP), anyString(), eq(Duration.ofMillis(10_000)), eq(RECORD_ID));
        }

        private void givenHandleFails() {
            givenStatus(null);
            givenStartProcess(true);
            willThrow(new RuntimeException("db down"))
                    .given(userAccountService).createAccountIfAbsent(USERNAME);
        }

        @Test
        @DisplayName("pending이 null이면 아무것도 하지 않는다")
        void whenPendingNull_doesNothing() {
            givenPending(null);

            consumer.retry();

            verify(streamOps, never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @Test
        @DisplayName("pending이 비어 있으면 아무것도 하지 않는다")
        void whenPendingEmpty_doesNothing() {
            givenPending(new PendingMessages(GROUP, List.of()));

            consumer.retry();

            verify(streamOps, never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @Test
        @DisplayName("마지막 전달 후 대기 시간이 기준 미만이면 claim하지 않는다")
        void whenNotIdleEnough_skips() {
            givenPending(pendingWithIdle(Duration.ofSeconds(1)));

            consumer.retry();

            verify(streamOps, never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @Test
        @DisplayName("claim한 레코드가 이미 DONE이면 ack만 한다")
        void whenClaimedDone_acks() {
            givenClaimed();
            givenStatus("DONE");

            consumer.retry();

            verifyNoInteractions(userAccountService);
            verifyAcked();
        }

        @Test
        @DisplayName("claim한 레코드가 PROCESSING이면 건너뛴다")
        void whenClaimedProcessing_skips() {
            givenClaimed();
            givenStatus("PROCESSING");

            consumer.retry();

            verifyNoInteractions(userAccountService);
            verifyNotAcked();
        }

        @Test
        @DisplayName("claim한 레코드의 선점에 실패하면 건너뛴다")
        void whenClaimedStartProcessFails_skips() {
            givenClaimed();
            givenStatus(null);
            givenStartProcess(false);

            consumer.retry();

            verifyNoInteractions(userAccountService);
            verifyNotAcked();
        }

        @Test
        @DisplayName("재처리에 성공하면 DONE 마킹, ack 후 retry 카운트를 삭제한다")
        void whenRetrySucceeds_acksAndClearsRetryCount() {
            givenClaimed();
            givenStatus(null);
            givenStartProcess(true);

            consumer.retry();

            verify(userAccountService).createAccountIfAbsent(USERNAME);
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
            assertThat(dlqRecord.getStream()).isEqualTo("user-create-dlq");
            assertThat(((Map<Object, Object>) dlqRecord.getValue()))
                    .containsEntry("recordId", "1-0")
                    .containsEntry("original", Map.of("type", "USER_CREATED", "username", USERNAME))
                    .containsKey("failedAt");

            verifyAcked();
            verify(streamRedisTemplate).delete(RETRY_KEY);
            verify(valueOps, never()).increment(anyString());
        }
    }

    // ===================== 방어 코드 =====================

    @Test
    @DisplayName("recordId가 null이면 상태 조회는 null, 선점은 false를 반환한다")
    void nullRecordId_guards() {
        String status = ReflectionTestUtils.invokeMethod(consumer, "getStatus", new Object[]{null});
        Boolean started = ReflectionTestUtils.invokeMethod(consumer, "tryStartProcess", new Object[]{null});

        assertThat(status).isNull();
        assertThat(started).isFalse();
        verifyNoInteractions(valueOps);
    }
}
