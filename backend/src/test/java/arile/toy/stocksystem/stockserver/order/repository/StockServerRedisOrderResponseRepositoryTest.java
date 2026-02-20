package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServerRedisOrderResponseRepositoryTest {

    @Mock
    private RedisTemplate<String, StockServerOrderResponseMessage> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private StockServerRedisOrderResponseRepository repository;

    private final String username = "user1";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("Order 응답 메시지를 저장할 때 Redis Hash put이 호출된다")
    void givenOrderResponse_whenSave_thenPutCalled() {
        // given
        StockServerOrderResponseMessage message = new StockServerOrderResponseMessage(
                1L, username, "005930", OrderType.BUY, 50000,
                50, 50, Instant.now()
        );

        // when
        repository.save(message);

        // then
        verify(hashOperations).put(
                eq("user:order:" + username),
                eq("1"),
                eq(message)
        );
    }

    @Test
    @DisplayName("주어진 username과 orderId로 삭제 시 Redis Hash delete가 호출된다")
    void givenUsernameAndOrderId_whenDelete_thenDeleteCalled() {
        // when
        repository.delete(username, 1L);

        // then
        verify(hashOperations).delete(
                eq("user:order:" + username),
                eq("1")
        );
    }

    @Test
    @DisplayName("주어진 username과 orderId로 업데이트 시 Redis Hash put이 호출된다")
    void givenUsernameAndOrderIdAndNewValue_whenUpdate_thenPutCalled() {
        // given
        StockServerOrderResponseMessage newMessage = new StockServerOrderResponseMessage(
                1L, username, "005930", OrderType.BUY, 50000,
                50, 50, Instant.now()
        );

        // when
        repository.update(username, 1L, newMessage);

        // then
        verify(hashOperations).put(
                eq("user:order:" + username),
                eq("1"),
                eq(newMessage)
        );
    }
}
