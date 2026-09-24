package arile.toy.stocksystem.bffserver.cancel.event.publisher;

import arile.toy.stocksystem.bffserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisCancelStreamShardResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisCancelRequestEventPublisherTest {

    private StreamOperations<String, Object, Object> streamOps;
    private RedisCancelStreamShardResolver shardResolver;
    private RedisCancelRequestEventPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();
        shardResolver = mock(RedisCancelStreamShardResolver.class);

        publisher = new RedisCancelRequestEventPublisher(redisTemplate, shardResolver);
    }

    @Test
    @DisplayName("종목코드로 샤드 스트림을 정하고, 주문 ID·종목코드·요청자 사용자명을 담아 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishCancel() {
        given(shardResolver.resolveStreamKey("005930")).willReturn("cancel-events-A");
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        publisher.publishCancel(new CancelRequestEvent(1L, "005930", "user1"));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        MapRecord record = captor.getValue();
        assertThat(record.getStream()).isEqualTo("cancel-events-A");
        assertThat((Map<Object, Object>) record.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "CANCEL_CREATED",
                "orderId", "1",
                "stockCode", "005930",
                "username", "user1"));
    }
}
