package arile.toy.stocksystem.bffserver.order.event.publisher;

import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.OrderRequestEvent;
import arile.toy.stocksystem.bffserver.sharding.RedisOrderStreamShardResolver;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisOrderRequestEventPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private RedisOrderStreamShardResolver shardResolver;

    @InjectMocks
    private RedisOrderRequestEventPublisher publisher;

    @Captor
    private ArgumentCaptor<MapRecord<String, Object, Object>> captor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    @DisplayName("주문 이벤트를 Redis Stream에 정상 발행")
    void givenOrderRequestEvent_whenPublishOrder_thenAddsToRedisStream() {

        // given
        OrderRequestEvent event = new OrderRequestEvent(
                "user1",
                "005930",
                OrderType.BUY,
                70000,
                10
        );

        String streamKey = "order-stream-1";

        when(shardResolver.resolveStreamKey("005930"))
                .thenReturn(streamKey);

        when(streamOperations.add(any()))
                .thenReturn(RecordId.of("123-0"));

        // when
        publisher.publishOrder(event);

        // then
        verify(shardResolver).resolveStreamKey("005930");
        verify(streamOperations).add(captor.capture());

        MapRecord<String, Object, Object> record = captor.getValue();
        Map<Object, Object> value = record.getValue();

        assertThat(record.getStream()).isEqualTo(streamKey);
        assertThat(value).containsEntry("type", "ORDER_CREATED");
        assertThat(value).containsEntry("username", "user1");
        assertThat(value).containsEntry("stockCode", "005930");
        assertThat(value).containsEntry("orderType", "BUY");
        assertThat(value).containsEntry("orderPrice", "70000");
        assertThat(value).containsEntry("orderQuantity", "10");
    }
}
