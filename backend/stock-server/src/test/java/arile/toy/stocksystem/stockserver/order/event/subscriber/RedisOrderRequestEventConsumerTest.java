package arile.toy.stocksystem.stockserver.order.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] 주문 요청 스트림 컨슈머 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOrderRequestEventConsumerTest {

    private static final String STREAM_KEY = "order-1";
    private static final String GROUP = "order-group";
    private static final RecordId RECORD_ID = RecordId.of("1-0");
    private static final String PROCESSED_KEY = "processed:order:1-0";
    private static final String RETRY_KEY = "retry:1-0";

    @InjectMocks private RedisOrderRequestEventConsumer sut;

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private OrderService orderService;
    @Mock private StockServerMarketPhaseRegistry registry;

    @Mock private StreamOperations<String, Object, Object> streamOps;
    @Mock private ValueOperations<String, Object> valueOps;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "prefix", "order");
        ReflectionTestUtils.setField(sut, "group", GROUP);
        ReflectionTestUtils.setField(sut, "stockGroup", "1");

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

            then(orderService).shouldHaveNoInteractions();
            then(valueOps).shouldHaveNoInteractions();
        }

        @DisplayName("ORDER_CREATED가 아닌 레코드는 처리 없이 ack한다")
        @Test
        void givenOtherType_whenConsuming_thenAcksWithoutProcessing() {
            Map<Object, Object> value = orderValue();
            value.put("type", "SOMETHING_ELSE");
            givenRead(List.of(record(value)));

            sut.consume();

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(orderService).shouldHaveNoInteractions();
        }

        @DisplayName("이미 DONE인 레코드는 중복 처리하지 않고 ack한다")
        @Test
        void givenDone_whenConsuming_thenSkipsAndAcks() {
            givenRead(List.of(record(orderValue())));
            given(valueOps.get(PROCESSED_KEY)).willReturn("DONE");

            sut.consume();

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(orderService).shouldHaveNoInteractions();
        }

        @DisplayName("다른 곳에서 PROCESSING 중인 레코드는 건너뛰고 ack하지 않는다")
        @Test
        void givenProcessing_whenConsuming_thenSkipsWithoutAck() {
            givenRead(List.of(record(orderValue())));
            given(valueOps.get(PROCESSED_KEY)).willReturn("PROCESSING");

            sut.consume();

            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
            then(orderService).shouldHaveNoInteractions();
        }

        @DisplayName("PROCESSING 선점에 실패하면 처리하지 않는다")
        @Test
        void givenAcquireFails_whenConsuming_thenSkips() {
            givenRead(List.of(record(orderValue())));
            given(valueOps.setIfAbsent(PROCESSED_KEY, "PROCESSING", Duration.ofMinutes(5))).willReturn(false);

            sut.consume();

            then(orderService).shouldHaveNoInteractions();
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("정상 레코드는 파싱해 주문을 등록하고 DONE 기록 후 ack한다")
        @Test
        void givenValidRecord_whenConsuming_thenRegistersOrderAndAcks() {
            givenRead(List.of(record(orderValue())));
            givenAcquire();

            sut.consume();

            ArgumentCaptor<StockServerOrderRequestEvent> captor =
                    ArgumentCaptor.forClass(StockServerOrderRequestEvent.class);
            then(orderService).should().registerOrder(captor.capture(), eq(false));
            StockServerOrderRequestEvent event = captor.getValue();
            assertThat(event.username()).isEqualTo("user");
            assertThat(event.stockCode()).isEqualTo("005930");
            assertThat(event.orderType()).isEqualTo(OrderType.BUY);
            assertThat(event.orderPrice()).isEqualTo(70_000);
            assertThat(event.orderQuantity()).isEqualTo(10);
            assertThat(event.leverageRatio()).isEqualTo(LeverageRatio.X2);
            assertThat(event.orderExecutionType()).isEqualTo(OrderExecutionType.MARKET);

            then(valueOps).should().set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
        }

        @DisplayName("레버리지·체결유형이 없으면 SPOT·LIMIT으로 간주하고, 소문자 값도 파싱한다")
        @Test
        void givenMissingOptionalFields_whenConsuming_thenUsesDefaults() {
            Map<Object, Object> value = orderValue();
            value.remove("leverageRatio");
            value.remove("orderExecutionType");
            value.put("orderType", "sell");
            givenRead(List.of(record(value)));
            givenAcquire();

            sut.consume();

            ArgumentCaptor<StockServerOrderRequestEvent> captor =
                    ArgumentCaptor.forClass(StockServerOrderRequestEvent.class);
            then(orderService).should().registerOrder(captor.capture(), eq(false));
            assertThat(captor.getValue().orderType()).isEqualTo(OrderType.SELL);
            assertThat(captor.getValue().leverageRatio()).isEqualTo(LeverageRatio.SPOT);
            assertThat(captor.getValue().orderExecutionType()).isEqualTo(OrderExecutionType.LIMIT);
        }

        @DisplayName("주문유형·레버리지·체결유형 값이 잘못되면 주문을 등록하지 않고 처리 완료로 ack한다")
        @Test
        void givenInvalidEnumValues_whenConsuming_thenSkipsOrderAndAcks() {
            Map<Object, Object> invalidOrderType = orderValue();
            invalidOrderType.put("orderType", "HOLD");
            Map<Object, Object> invalidLeverage = orderValue();
            invalidLeverage.put("leverageRatio", "X10");
            Map<Object, Object> invalidExecution = orderValue();
            invalidExecution.put("orderExecutionType", "STOP");

            givenRead(List.of(
                    record("1-0", invalidOrderType),
                    record("2-0", invalidLeverage),
                    record("3-0", invalidExecution)));
            given(valueOps.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).willReturn(true);

            sut.consume();

            then(orderService).shouldHaveNoInteractions();
            then(streamOps).should(times(3)).acknowledge(eq(STREAM_KEY), eq(GROUP), any(RecordId.class));
        }

        @DisplayName("장이 닫힌 종목이면 주문을 등록하지 않고 처리 완료로 ack한다")
        @Test
        void givenMarketClosed_whenConsuming_thenSkipsOrderAndAcks() {
            givenRead(List.of(record(orderValue())));
            givenAcquire();
            given(registry.isClosed("005930")).willReturn(true);

            sut.consume();

            then(orderService).shouldHaveNoInteractions();
            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
        }

        @DisplayName("주문 등록이 실패하면 PROCESSING 표시를 지우고 ack하지 않아 재시도 대상으로 남긴다")
        @Test
        void givenRegisterFails_whenConsuming_thenClearsProcessingAndDoesNotAck() {
            givenRead(List.of(record(orderValue())));
            givenAcquire();
            given(orderService.registerOrder(any(), eq(false))).willThrow(new IllegalStateException("fail"));

            sut.consume();

            then(streamRedisTemplate).should().delete(PROCESSED_KEY);
            then(valueOps).should(never()).set(anyString(), eq("DONE"), any(Duration.class));
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("orderType이 없으면 예외로 처리되어 재시도 대상으로 남는다")
        @Test
        void givenMissingOrderType_whenConsuming_thenLeftForRetry() {
            Map<Object, Object> value = orderValue();
            value.remove("orderType");
            givenRead(List.of(record(value)));
            givenAcquire();

            sut.consume();

            then(orderService).shouldHaveNoInteractions();
            then(streamRedisTemplate).should().delete(PROCESSED_KEY);
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("DONE 기록 후 ack만 실패하면 DONE 표시를 지우지 않는다 (중복 주문 방지)")
        @Test
        void givenAckFailsAfterDone_whenConsuming_thenKeepsDoneMark() {
            givenRead(List.of(record(orderValue())));
            givenAcquire();
            given(streamOps.acknowledge(STREAM_KEY, GROUP, RECORD_ID)).willThrow(new IllegalStateException("redis"));

            sut.consume();

            then(valueOps).should().set(PROCESSED_KEY, "DONE", Duration.ofHours(24));
            then(streamRedisTemplate).should(never()).delete(anyString());
        }

        @DisplayName("상태 조회 자체가 실패하면 선점한 적이 없으므로 표시를 지우지 않는다")
        @Test
        void givenStatusLookupFails_whenConsuming_thenDoesNotClearMark() {
            givenRead(List.of(record(orderValue())));
            given(valueOps.get(PROCESSED_KEY)).willThrow(new IllegalStateException("redis"));

            sut.consume();

            then(streamRedisTemplate).should(never()).delete(anyString());
            then(orderService).shouldHaveNoInteractions();
        }

        @DisplayName("한 레코드가 실패해도 다음 레코드는 계속 처리한다")
        @Test
        void givenFirstRecordFails_whenConsuming_thenContinuesWithNext() {
            givenRead(List.of(record("1-0", orderValue()), record("2-0", orderValue())));
            given(valueOps.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).willReturn(true);
            given(orderService.registerOrder(any(), eq(false)))
                    .willThrow(new IllegalStateException("fail"))
                    .willReturn(null);

            sut.consume();

            then(orderService).should(times(2)).registerOrder(any(), eq(false));
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

        @DisplayName("마지막 전달 후 10초가 지나지 않은 메시지는 가져오지 않는다")
        @Test
        void givenRecentlyDelivered_whenRetrying_thenDoesNotClaim() {
            givenPending(List.of(pendingMessage(Duration.ofSeconds(5))));

            sut.retry();

            then(streamOps).should(never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId[].class));
        }

        @DisplayName("재처리에 성공하면 주문을 등록하고 ack한 뒤 재시도 횟수를 지운다")
        @Test
        void givenClaimedRecord_whenRetrySucceeds_thenAcksAndClearsRetryCount() {
            givenClaimed(record(orderValue()));
            givenAcquire();

            sut.retry();

            then(orderService).should().registerOrder(any(), eq(false));
            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(streamRedisTemplate).should().delete(RETRY_KEY);
        }

        @DisplayName("재처리 시 이미 DONE이면 ack만 한다")
        @Test
        void givenDone_whenRetrying_thenOnlyAcks() {
            givenClaimed(record(orderValue()));
            given(valueOps.get(PROCESSED_KEY)).willReturn("DONE");

            sut.retry();

            then(streamOps).should().acknowledge(STREAM_KEY, GROUP, RECORD_ID);
            then(orderService).shouldHaveNoInteractions();
        }

        @DisplayName("재처리 시 PROCESSING 중이면 건너뛴다")
        @Test
        void givenProcessing_whenRetrying_thenSkips() {
            givenClaimed(record(orderValue()));
            given(valueOps.get(PROCESSED_KEY)).willReturn("PROCESSING");

            sut.retry();

            then(orderService).shouldHaveNoInteractions();
            then(streamOps).should(never()).acknowledge(anyString(), anyString(), any(RecordId[].class));
        }

        @DisplayName("재처리 실패 횟수가 한도 미만이면 PROCESSING 표시를 지우고 재시도 횟수를 올린다")
        @Test
        void givenFailureUnderLimit_whenRetrying_thenClearsMarkAndIncreasesCount() {
            givenClaimed(record(orderValue()));
            givenAcquire();
            given(orderService.registerOrder(any(), eq(false))).willThrow(new IllegalStateException("fail"));
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
            Map<Object, Object> value = orderValue();
            givenClaimed(record(value));
            givenAcquire();
            given(orderService.registerOrder(any(), eq(false))).willThrow(new IllegalStateException("fail"));
            given(valueOps.get(PROCESSED_KEY)).willReturn(null);
            given(valueOps.get(RETRY_KEY)).willReturn("3");

            sut.retry();

            ArgumentCaptor<MapRecord> dlqCaptor = ArgumentCaptor.forClass(MapRecord.class);
            then(streamOps).should().add(dlqCaptor.capture());
            MapRecord dlqRecord = dlqCaptor.getValue();
            assertThat(dlqRecord.getStream()).isEqualTo("order-dlq");
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
            givenClaimed(record(orderValue()));
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

    private Map<Object, Object> orderValue() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "ORDER_CREATED");
        value.put("username", "user");
        value.put("stockCode", "005930");
        value.put("orderType", "BUY");
        value.put("orderPrice", "70000");
        value.put("orderQuantity", "10");
        value.put("leverageRatio", "X2");
        value.put("orderExecutionType", "MARKET");
        return value;
    }

    private MapRecord<String, Object, Object> record(Map<Object, Object> value) {
        return record("1-0", value);
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
