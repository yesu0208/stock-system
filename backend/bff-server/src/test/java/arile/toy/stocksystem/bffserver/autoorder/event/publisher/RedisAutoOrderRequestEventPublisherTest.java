package arile.toy.stocksystem.bffserver.autoorder.event.publisher;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.sharding.RedisAutoOrderStreamShardResolver;
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

class RedisAutoOrderRequestEventPublisherTest {

    @Test
    @DisplayName("종목코드로 샤드 스트림을 정하고, 자동 주문 정보를 문자열 필드로 담아 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishAutoOrder() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();
        RedisAutoOrderStreamShardResolver shardResolver = mock(RedisAutoOrderStreamShardResolver.class);
        given(shardResolver.resolveStreamKey("005930")).willReturn("auto-order-events-A");
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        new RedisAutoOrderRequestEventPublisher(redisTemplate, shardResolver).publishAutoOrder(new AutoOrderRequestEvent(
                "user1", "005930", AutoOrderType.BUY, 68_000, 69_000, 10, LeverageRatio.X1_5));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        assertThat(captor.getValue().getStream()).isEqualTo("auto-order-events-A");
        assertThat((Map<Object, Object>) captor.getValue().getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "type", "AUTO_ORDER_CREATED",
                "username", "user1",
                "stockCode", "005930",
                "autoOrderType", "BUY",
                "triggerPrice", "68000",
                "orderPrice", "69000",
                "orderQuantity", "10",
                "leverageRatio", "X1_5"));
    }
}
