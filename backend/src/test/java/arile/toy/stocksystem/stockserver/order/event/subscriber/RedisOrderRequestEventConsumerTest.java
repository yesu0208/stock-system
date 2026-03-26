package arile.toy.stocksystem.stockserver.order.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
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
class RedisOrderRequestEventConsumerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @Mock
    private OrderService orderService;

    @Mock
    private StockServerMarketPhaseRegistry registry;

    private RedisOrderRequestEventConsumer consumer;

    private final String prefix = "order-stream";
    private final String group = "order-group";
    private final int shardIndex = 0;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);

        consumer = new RedisOrderRequestEventConsumer(redisTemplate, orderService, registry);

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
        verifyNoInteractions(orderService);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("ORDER_CREATED 이벤트 처리")
    void givenOrderRecord_whenConsume_thenRegisterOrder() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "ORDER_CREATED");
        value.put("username", "user1");
        value.put("stockCode", "TEST");
        value.put("orderType", "BUY");
        value.put("orderPrice", 1000);
        value.put("orderQuantity", 5);

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

        verify(orderService).registerOrder(argThat(event ->
                event.username().equals("user1") &&
                        event.stockCode().equals("TEST") &&
                        event.orderType().name().equals("BUY") &&
                        event.orderPrice().equals(1000) &&
                        event.orderQuantity().equals(5)
        ), eq(false));

        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("ORDER_CREATED 아닌 이벤트는 무시")
    void givenOtherEvent_whenConsume_thenDoNothing() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTHER_EVENT");
        value.put("username", "user1");
        value.put("stockCode", "TEST");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any()))
                .thenReturn(List.of(record));

        consumer.consume();

        verifyNoInteractions(orderService);
        verify(streamOps).acknowledge(prefix + "-" + shardIndex, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("ORDER_CREATED 이벤트 직접 호출")
    void givenOrderRecord_whenHandle_thenRegisterOrder() throws Exception {
        Map<Object, Object> value = Map.of(
                "type", "ORDER_CREATED",
                "username", "user2",
                "stockCode", "TEST2",
                "orderType", "SELL",
                "orderPrice", 500,
                "orderQuantity", 2
        );

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);
        when(registry.isClosed("TEST2")).thenReturn(false);

        var handleMethod = RedisOrderRequestEventConsumer.class
                .getDeclaredMethod("handle", MapRecord.class);
        handleMethod.setAccessible(true);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                handleMethod.invoke(consumer, record)
        );

        verify(orderService).registerOrder(argThat(event ->
                event.username().equals("user2") &&
                        event.stockCode().equals("TEST2") &&
                        event.orderType().name().equals("SELL") &&
                        event.orderPrice().equals(500) &&
                        event.orderQuantity().equals(2)
        ), eq(false));
    }
}
