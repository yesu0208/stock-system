package arile.toy.stocksystem.stockserver.autoorder.repository;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
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

@DisplayName("[Repository] 미발동 자동주문 응답 Redis 저장소 테스트")
@ExtendWith(MockitoExtension.class)
class StockServerRedisAutoOrderResponseRepositoryTest {

    @InjectMocks private StockServerRedisAutoOrderResponseRepository sut;

    @Mock private RedisTemplate<String, StockServerAutoOrderResponseMessage> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOps;

    @BeforeEach
    void setUp() {
        doReturn(hashOps).when(redisTemplate).opsForHash();
    }

    @DisplayName("자동주문 응답을 사용자 해시(user:auto:order:{username})에 자동주문 ID 필드로 저장한다")
    @Test
    void whenSaving_thenPutsIntoUserHash() {
        var message = new StockServerAutoOrderResponseMessage(1L, "user", "005930", AutoOrderType.BUY,
                LeverageRatio.SPOT, 71_000, 70_000, 10, Instant.parse("2026-09-25T00:00:00Z"));

        sut.save(message);

        then(hashOps).should().put("user:auto:order:user", "1", message);
    }

    @DisplayName("사용자 해시에서 자동주문 ID 필드를 삭제한다")
    @Test
    void whenDeleting_thenRemovesField() {
        sut.delete("user", 1L);

        then(hashOps).should().delete("user:auto:order:user", "1");
    }
}
