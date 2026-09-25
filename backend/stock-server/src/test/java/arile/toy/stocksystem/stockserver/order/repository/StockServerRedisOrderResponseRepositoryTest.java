package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.*;
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

import static org.mockito.BDDMockito.*;

@DisplayName("[Repository] 미체결 주문 응답 Redis 저장소 테스트")
@ExtendWith(MockitoExtension.class)
class StockServerRedisOrderResponseRepositoryTest {

    private static final String KEY = "user:order:user";

    @InjectMocks private StockServerRedisOrderResponseRepository sut;

    @Mock private RedisTemplate<String, StockServerOrderResponseMessage> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOps;

    @BeforeEach
    void setUp() {
        doReturn(hashOps).when(redisTemplate).opsForHash();
    }

    @DisplayName("주문 응답을 사용자 해시에 주문 ID를 필드로 저장한다")
    @Test
    void givenMessage_whenSaving_thenPutsIntoUserHash() {
        var message = message(1L, 10);

        sut.save(message);

        then(hashOps).should().put(KEY, "1", message);
    }

    @DisplayName("사용자 해시에서 해당 주문 ID 필드를 삭제한다")
    @Test
    void givenUsernameAndOrderId_whenDeleting_thenRemovesField() {
        sut.delete("user", 1L);

        then(hashOps).should().delete(KEY, "1");
    }

    @DisplayName("사용자 해시의 해당 주문 ID 필드를 새 값으로 덮어쓴다")
    @Test
    void givenNewValue_whenUpdating_thenOverwritesField() {
        var partiallyFilled = message(1L, 3);

        sut.update("user", 1L, partiallyFilled);

        then(hashOps).should().put(KEY, "1", partiallyFilled);
    }

    private StockServerOrderResponseMessage message(Long orderId, int remainingQuantity) {
        return StockServerOrderResponseMessage.of(
                orderId, "user", "005930", OrderType.BUY, LeverageRatio.SPOT,
                70_000, 10, remainingQuantity, Instant.parse("2026-09-25T00:00:00Z"),
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }
}
