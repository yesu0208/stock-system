package arile.toy.stocksystem.stockserver.autoorder.repository;

import static org.mockito.Mockito.*;

import java.time.Instant;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class StockServerRedisAutoOrderResponseRepositoryTest {

    @Mock
    private RedisTemplate<String, StockServerAutoOrderResponseMessage> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private StockServerRedisAutoOrderResponseRepository repository;

    @Test
    @DisplayName("AutoOrderResponseMessage 저장 시 Redis Hash put 호출")
    void givenMessage_whenSave_thenCallsRedisPut() {
        // given
        StockServerAutoOrderResponseMessage message = new StockServerAutoOrderResponseMessage(
                1L, "user1", "005930", AutoOrderType.BUY, 50000,
                50000, 50, Instant.now()
        );

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // when
        repository.save(message);

        // then
        verify(hashOperations).put(
                "user:auto:order:" + message.username(),
                message.autoOrderId().toString(),
                message
        );
    }

    @Test
    @DisplayName("사용자 이름과 주문 ID로 삭제 시 Redis Hash delete 호출")
    void givenUsernameAndId_whenDelete_thenCallsRedisDelete() {
        // given
        String username = "user1";
        Long autoOrderId = 1L;

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // when
        repository.delete(username, autoOrderId);

        // then
        verify(hashOperations).delete("user:auto:order:" + username, autoOrderId.toString());
    }
}
