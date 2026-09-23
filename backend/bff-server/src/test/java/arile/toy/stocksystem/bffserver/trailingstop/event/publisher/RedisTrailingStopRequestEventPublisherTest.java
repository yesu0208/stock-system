package arile.toy.stocksystem.bffserver.trailingstop.event.publisher;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.sharding.RedisTrailingStopStreamShardResolver;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopRequestEvent;
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

class RedisTrailingStopRequestEventPublisherTest {

    @Test
    @DisplayName("종목코드로 샤드 스트림을 정하고, 트레일링 스탑 정보를 문자열 필드로 담아 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishTrailingStop() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();
        RedisTrailingStopStreamShardResolver shardResolver = mock(RedisTrailingStopStreamShardResolver.class);
        given(shardResolver.resolveStreamKey("005930")).willReturn("trailing-stop-events-A");
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        new RedisTrailingStopRequestEventPublisher(redisTemplate, shardResolver).publishTrailingStop(
                new TrailingStopRequestEvent("user1", "005930", TrailingStopType.SELL, 10, 3.5, 70_000, LeverageRatio.SPOT));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        assertThat(captor.getValue().getStream()).isEqualTo("trailing-stop-events-A");
        assertThat((Map<Object, Object>) captor.getValue().getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "TRAILING_STOP_CREATED",
                "username", "user1",
                "stockCode", "005930",
                "trailingStopType", "SELL",
                "orderQuantity", "10",
                "stopPercent", "3.5",
                "basePrice", "70000",
                "leverageRatio", "SPOT"));
    }
}
