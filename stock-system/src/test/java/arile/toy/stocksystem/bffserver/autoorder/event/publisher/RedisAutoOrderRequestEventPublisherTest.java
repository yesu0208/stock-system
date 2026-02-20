package arile.toy.stocksystem.bffserver.autoorder.event.publisher;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAutoOrderStreamShardResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAutoOrderRequestEventPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisAutoOrderStreamShardResolver shardResolver;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private RedisAutoOrderRequestEventPublisher publisher;

    @Captor
    private ArgumentCaptor<MapRecord<String, Object, Object>> captor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    @DisplayName("자동주문 이벤트 발생 시 Redis 스트림에 추가")
    void givenEvent_whenPublishAutoOrder_thenAddToRedisStream() {

        // given
        AutoOrderRequestEvent event =
                new AutoOrderRequestEvent(
                        "user1",
                        "005930",
                        AutoOrderType.BUY,
                        48000,
                        50000,
                        10
                );

        String streamKey = "auto-order:005930";

        when(shardResolver.resolveStreamKey("005930"))
                .thenReturn(streamKey);

        when(streamOperations.add(any()))
                .thenReturn(RecordId.of("1-0"));

        // when
        publisher.publishAutoOrder(event);

        // then
        verify(shardResolver).resolveStreamKey("005930");
        verify(streamOperations).add(captor.capture());

        MapRecord<String, Object, Object> record = captor.getValue();
        Map<Object, Object> value = record.getValue();

        assertEquals(streamKey, record.getStream());
        assertEquals("AUTO_ORDER_CREATED", value.get("type"));
        assertEquals("user1", value.get("username"));
        assertEquals("005930", value.get("stockCode"));
        assertEquals("BUY", value.get("autoOrderType"));
        assertEquals("48000", value.get("triggerPrice"));
        assertEquals("50000", value.get("orderPrice"));
        assertEquals("10", value.get("orderQuantity"));
    }
}
