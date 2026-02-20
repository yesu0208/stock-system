package arile.toy.stocksystem.stockserver.autocancel.event.subscriber;

import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAutoCancelRequestEventConsumerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @Mock
    private AutoCancelService autoCancelService;

    @Mock
    private StockServerMarketPhaseRegistry registry;

    private RedisAutoCancelRequestEventConsumer consumer;

    private final String prefix = "auto-cancel-stream";
    private final String group = "auto-cancel-group";
    private final int shardIndex = 0;

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);

        consumer = new RedisAutoCancelRequestEventConsumer(
                redisTemplate,
                autoCancelService,
                registry
        );

        setField(consumer, "prefix", prefix);
        setField(consumer, "group", group);
        setField(consumer, "shardIndex", shardIndex);
    }

    @Test
    @DisplayName("이벤트 없으면 아무 동작도 하지 않음")
    void givenNoRecords_whenConsume_thenDoNothing() {
        // given
        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(Collections.emptyList());

        // when
        consumer.consume();

        // then
        verify(streamOps).read(any(Consumer.class), any(StreamReadOptions.class), any());
        verifyNoInteractions(autoCancelService);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AUTO_CANCEL_CREATED 이벤트 처리")
    void givenAutoCancelRecord_whenConsume_thenRegisterAutoCancel() {
        // given
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "AUTO_CANCEL_CREATED");
        value.put("autoOrderId", 123L);
        value.put("stockCode", "TEST");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(List.of(record));

        when(registry.isClosed("TEST")).thenReturn(false);

        // when
        consumer.consume();

        // then
        verify(autoCancelService).registerAutoCancel(argThat(event ->
                event.autoOrderId().equals(123L) &&
                        event.stockCode().equals("TEST")
        ));
        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AUTO_CANCEL_CREATED 아닌 이벤트는 무시")
    void givenOtherEvent_whenConsume_thenDoNothing() {
        // given
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTHER_EVENT");
        value.put("autoOrderId", 123L);
        value.put("stockCode", "TEST");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(List.of(record));

        // when
        consumer.consume();

        // then
        verifyNoInteractions(autoCancelService);
        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AUTO_CANCEL_CREATED 이벤트 직접 호출")
    void givenAutoCancelRecord_whenHandle_thenRegisterAutoCancel() throws Exception {
        // given
        Map<Object, Object> value = Map.of(
                "type", "AUTO_CANCEL_CREATED",
                "autoOrderId", 456L,
                "stockCode", "TEST2"
        );
        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        when(registry.isClosed("TEST2")).thenReturn(false);

        var handleMethod = RedisAutoCancelRequestEventConsumer.class
                .getDeclaredMethod("handle", MapRecord.class);
        handleMethod.setAccessible(true);

        // when
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                handleMethod.invoke(consumer, record)
        );

        // then
        verify(autoCancelService).registerAutoCancel(argThat(event ->
                event.autoOrderId().equals(456L) &&
                        event.stockCode().equals("TEST2")
        ));
    }
}
