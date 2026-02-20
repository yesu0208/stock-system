package arile.toy.stocksystem.stockserver.cancel.event.publisher;

import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCancelResponseEventPublisherTest {

    @Mock
    private RedisTemplate<String, CancelResponseEvent> redisTemplate;

    @InjectMocks
    private RedisCancelResponseEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<CancelResponseEvent> eventCaptor;

    @Test
    @DisplayName("Cancel 응답 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenCancelResponseEvent_whenPublish_thenSendsToRedis() {

        // given
        OrderEntity order = new OrderEntity();
        order.setOrderId(1L);
        order.setUsername("user1");
        order.setStockCode("005930");
        order.setOrderType(OrderType.BUY);
        order.setOrderPrice(70000);
        order.setOrderQuantity(10);

        CancelResponseEvent event = CancelResponseEvent.of(order, true, null);

        // when
        publisher.publish(event);

        // then
        verify(redisTemplate).convertAndSend(
                channelCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(channelCaptor.getValue())
                .isEqualTo("user:cancel.user1:event");

        assertThat(eventCaptor.getValue())
                .isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenRedisThrowsException_whenPublishCancelEvent_thenDoNotThrow() {

        // given
        OrderEntity order = new OrderEntity();
        order.setOrderId(1L);
        order.setUsername("user1");
        order.setStockCode("005930");
        order.setOrderType(OrderType.BUY);
        order.setOrderPrice(70000);
        order.setOrderQuantity(10);

        CancelResponseEvent event = CancelResponseEvent.of(order, true, null);

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        // when & then
        assertDoesNotThrow(() -> publisher.publish(event));
    }
}
