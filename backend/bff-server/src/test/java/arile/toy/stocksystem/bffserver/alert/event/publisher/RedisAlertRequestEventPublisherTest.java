package arile.toy.stocksystem.bffserver.alert.event.publisher;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisAlertStreamShardResolver;
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

class RedisAlertRequestEventPublisherTest {

    @Test
    @DisplayName("종목코드로 샤드 스트림을 정하고, 알림 정보를 문자열 필드로 담아 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishAlert() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();
        RedisAlertStreamShardResolver shardResolver = mock(RedisAlertStreamShardResolver.class);
        given(shardResolver.resolveStreamKey("005930")).willReturn("alert-events-A");
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        new RedisAlertRequestEventPublisher(redisTemplate, shardResolver)
                .publishAlert(new AlertRequestEvent("user1", "005930", AlertDirection.ABOVE, 70_000));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        assertThat(captor.getValue().getStream()).isEqualTo("alert-events-A");
        assertThat((Map<Object, Object>) captor.getValue().getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "ALERT_CREATED",
                "username", "user1",
                "stockCode", "005930",
                "direction", "ABOVE",
                "triggerPrice", "70000"));
    }
}
