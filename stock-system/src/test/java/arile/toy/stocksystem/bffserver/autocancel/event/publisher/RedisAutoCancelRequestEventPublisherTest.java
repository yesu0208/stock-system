package arile.toy.stocksystem.bffserver.autocancel.event.publisher;

import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAutoCancelStreamShardResolver;
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
class RedisAutoCancelRequestEventPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisAutoCancelStreamShardResolver shardResolver;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private RedisAutoCancelRequestEventPublisher publisher;

    @Captor
    private ArgumentCaptor<MapRecord<String, Object, Object>> captor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    @DisplayName("자동취소 이벤트 발생 시 Redis 스트림에 추가")
    void givenEvent_whenPublishAutoCancel_thenAddToRedisStream() {

        AutoCancelRequestEvent event =
                new AutoCancelRequestEvent(100L, "005930");

        String streamKey = "auto-cancel:005930";

        when(shardResolver.resolveStreamKey("005930"))
                .thenReturn(streamKey);

        when(streamOperations.add(any()))
                .thenReturn(RecordId.of("1-0"));

        // when
        publisher.publishAutoCancel(event);

        // then
        verify(streamOperations).add(captor.capture());

        MapRecord<String, Object, Object> record = captor.getValue();
        Map<Object, Object> value = record.getValue();

        assertEquals(streamKey, record.getStream());
        assertEquals("AUTO_CANCEL_CREATED", value.get("type"));
        assertEquals("100", value.get("autoOrderId"));
        assertEquals("005930", value.get("stockCode"));
    }
}
