package arile.toy.stocksystem.bffserver.otococancel.event.publisher;

import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisOtocoCancelStreamShardResolver;
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

class RedisOtocoCancelRequestEventPublisherTest {

    @Test
    @DisplayName("종목코드로 샤드 스트림을 정하고, OTOCO ID·종목코드·요청자 사용자명을 담아 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishOtocoCancel() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();
        RedisOtocoCancelStreamShardResolver shardResolver = mock(RedisOtocoCancelStreamShardResolver.class);
        given(shardResolver.resolveStreamKey("005930")).willReturn("otoco-cancel-events-A");
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        new RedisOtocoCancelRequestEventPublisher(redisTemplate, shardResolver)
                .publishOtocoCancel(new OtocoCancelRequestEvent(1L, "005930", "user1"));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        assertThat(captor.getValue().getStream()).isEqualTo("otoco-cancel-events-A");
        assertThat((Map<Object, Object>) captor.getValue().getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "OTOCO_CANCEL_CREATED",
                "otocoId", "1",
                "stockCode", "005930",
                "username", "user1"));
    }
}
