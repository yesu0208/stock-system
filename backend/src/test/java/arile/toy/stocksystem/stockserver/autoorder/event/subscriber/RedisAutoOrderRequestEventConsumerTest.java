package arile.toy.stocksystem.stockserver.autoorder.event.subscriber;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderService;
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
class RedisAutoOrderRequestEventConsumerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @Mock
    private AutoOrderService autoOrderService;

    @Mock
    private StockServerMarketPhaseRegistry registry;

    private RedisAutoOrderRequestEventConsumer consumer;

    private final String prefix = "auto-order-stream";
    private final String group = "auto-order-group";
    private final int shardIndex = 0;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);

        consumer = new RedisAutoOrderRequestEventConsumer(
                redisTemplate,
                autoOrderService,
                registry
        );

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
        verifyNoInteractions(autoOrderService);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AUTO_ORDER_CREATED 이벤트 처리")
    void givenAutoOrderRecord_whenConsume_thenRegisterAutoOrder() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "AUTO_ORDER_CREATED");
        value.put("username", "testuser");
        value.put("stockCode", "TEST");
        value.put("autoOrderType", "BUY");
        value.put("orderPrice", 1000);
        value.put("triggerPrice", 900);
        value.put("orderQuantity", 10);

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

        verify(autoOrderService).registerAutoOrder(argThat(event ->
                event.username().equals("testuser") &&
                        event.stockCode().equals("TEST") &&
                        event.autoOrderType() == AutoOrderType.BUY &&
                        event.orderPrice() == 1000 &&
                        event.triggerPrice() == 900 &&
                        event.orderQuantity() == 10
        ));

        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AUTO_ORDER_CREATED 아닌 이벤트는 무시")
    void givenOtherEvent_whenConsume_thenDoNothing() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTHER_EVENT");
        value.put("username", "testuser");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(List.of(record));

        consumer.consume();

        verifyNoInteractions(autoOrderService);
        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AUTO_ORDER_CREATED 이벤트 직접 호출")
    void givenAutoOrderRecord_whenHandle_thenRegisterAutoOrder() throws Exception {
        Map<Object, Object> value = Map.of(
                "type", "AUTO_ORDER_CREATED",
                "username", "user1",
                "stockCode", "TEST2",
                "autoOrderType", "SELL",
                "orderPrice", 2000,
                "triggerPrice", 1800,
                "orderQuantity", 5
        );

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        when(registry.isClosed("TEST2")).thenReturn(false);

        var handleMethod = RedisAutoOrderRequestEventConsumer.class
                .getDeclaredMethod("handle", MapRecord.class);
        handleMethod.setAccessible(true);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                handleMethod.invoke(consumer, record)
        );

        verify(autoOrderService).registerAutoOrder(argThat(event ->
                event.username().equals("user1") &&
                        event.stockCode().equals("TEST2") &&
                        event.autoOrderType() == AutoOrderType.SELL &&
                        event.orderPrice() == 2000 &&
                        event.triggerPrice() == 1800 &&
                        event.orderQuantity() == 5
        ));
    }
}
