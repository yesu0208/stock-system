package arile.toy.stocksystem.bffserver.otoco.event.publisher;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoExitMode;
import arile.toy.stocksystem.bffserver.otoco.event.OtocoRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisOtocoStreamShardResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisOtocoRequestEventPublisherTest {

    @Test
    @DisplayName("종목코드로 샤드 스트림을 정하고, 쓰지 않는 모드의 값은 \"null\" 문자열로 담아 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishOtoco() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();
        RedisOtocoStreamShardResolver shardResolver = mock(RedisOtocoStreamShardResolver.class);
        given(shardResolver.resolveStreamKey("005930")).willReturn("otoco-events-A");
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        new RedisOtocoRequestEventPublisher(redisTemplate, shardResolver).publishOtoco(new OtocoRequestEvent(
                "user1", "005930", OtocoEntryDirection.ABOVE, 10, 70_000,
                OtocoExitMode.PRICE, 75_000, null, OtocoExitMode.PCT, null, 3.0, LeverageRatio.X1_5));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        assertThat(captor.getValue().getStream()).isEqualTo("otoco-events-A");
        assertThat((Map<Object, Object>) captor.getValue().getValue()).containsOnly(
                entry("type", "OTOCO_CREATED"),
                entry("username", "user1"),
                entry("stockCode", "005930"),
                entry("entryDirection", "ABOVE"),
                entry("orderQuantity", "10"),
                entry("entryTriggerPrice", "70000"),
                entry("tpMode", "PRICE"),
                entry("tpPrice", "75000"),
                entry("tpPct", "null"),
                entry("slMode", "PCT"),
                entry("slPrice", "null"),
                entry("slPct", "3.0"),
                entry("leverageRatio", "X1_5"));
    }
}
