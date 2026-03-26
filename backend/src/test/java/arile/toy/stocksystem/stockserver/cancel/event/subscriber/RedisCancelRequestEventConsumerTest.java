package arile.toy.stocksystem.stockserver.cancel.event.subscriber;

import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCancelRequestEventConsumerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @Mock
    private CancelService cancelService;

    @Mock
    private StockServerMarketPhaseRegistry registry;

    private RedisCancelRequestEventConsumer consumer;

    private final String prefix = "cancel-stream";
    private final String group = "cancel-group";
    private final int shardIndex = 0;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);

        consumer = new RedisCancelRequestEventConsumer(redisTemplate, cancelService, registry);

        ReflectionTestUtils.setField(consumer, "prefix", prefix);
        ReflectionTestUtils.setField(consumer, "group", group);
        ReflectionTestUtils.setField(consumer, "shardIndex", shardIndex);
    }

    @Test
    @DisplayName("이벤트 없으면 아무 동작도 하지 않음")
    void givenNoRecords_whenConsume_thenDoNothing() {
        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(Collections.emptyList());

        consumer.consume();

        verify(streamOps).read(any(Consumer.class), any(StreamReadOptions.class), any());
        verifyNoInteractions(cancelService);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("CANCEL_CREATED 이벤트 처리")
    void givenCancelRecord_whenConsume_thenRegisterCancel() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "CANCEL_CREATED");
        value.put("orderId", 123L);
        value.put("stockCode", "TEST");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);

        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(List.of(record));
        when(streamOps.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);
        doNothing().when(valueOps).set(anyString(), eq("DONE"), any(Duration.class));


        when(registry.isClosed("TEST")).thenReturn(false);

        consumer.consume();

        verify(cancelService).registerCancel(argThat(event ->
                event.orderId().equals(123L) &&
                        event.stockCode().equals("TEST")
        ));

        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("CANCEL_CREATED 아닌 이벤트는 무시")
    void givenOtherEvent_whenConsume_thenDoNothing() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTHER_EVENT");
        value.put("orderId", 123L);
        value.put("stockCode", "TEST");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(List.of(record));

        consumer.consume();

        verifyNoInteractions(cancelService);
        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("CANCEL_CREATED 이벤트 직접 호출")
    void givenCancelRecord_whenHandle_thenRegisterCancel() throws Exception {
        Map<Object, Object> value = Map.of(
                "type", "CANCEL_CREATED",
                "orderId", 456L,
                "stockCode", "TEST2"
        );

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        when(registry.isClosed("TEST2")).thenReturn(false);

        // private handle() reflection 호출
        var handleMethod = RedisCancelRequestEventConsumer.class
                .getDeclaredMethod("handle", MapRecord.class);
        handleMethod.setAccessible(true);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                handleMethod.invoke(consumer, record)
        );

        verify(cancelService).registerCancel(argThat(event ->
                event.orderId().equals(456L) &&
                        event.stockCode().equals("TEST2")
        ));
    }
}
