package arile.toy.stocksystem.bffserver.cancel.event.publisher;

import arile.toy.stocksystem.bffserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisCancelStreamShardResolver;
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
class RedisCancelRequestEventPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisCancelStreamShardResolver shardResolver;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private RedisCancelRequestEventPublisher publisher;

    @Captor
    private ArgumentCaptor<MapRecord<String, Object, Object>> captor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    @DisplayName("주문 취소 이벤트 발생 시 Redis 스트림에 추가")
    void givenEvent_whenPublishCancel_thenAddToRedisStream() {

        // given
        CancelRequestEvent event =
                new CancelRequestEvent(1L, "005930");

        String streamKey = "cancel:005930";

        when(shardResolver.resolveStreamKey("005930"))
                .thenReturn(streamKey);

        when(streamOperations.add(any()))
                .thenReturn(RecordId.of("1-0"));

        // when
        publisher.publishCancel(event);

        // then
        verify(shardResolver).resolveStreamKey("005930");
        verify(streamOperations).add(captor.capture());

        MapRecord<String, Object, Object> record = captor.getValue();
        Map<Object, Object> value = record.getValue();

        assertEquals(streamKey, record.getStream());
        assertEquals("CANCEL_CREATED", value.get("type"));
        assertEquals("1", value.get("orderId"));
        assertEquals("005930", value.get("stockCode"));
    }
}
