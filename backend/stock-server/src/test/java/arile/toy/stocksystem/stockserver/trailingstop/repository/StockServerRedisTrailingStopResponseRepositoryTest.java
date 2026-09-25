package arile.toy.stocksystem.stockserver.trailingstop.repository;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
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

@DisplayName("[Repository] 트레일링 스탑 응답 Redis 저장소 테스트")
@ExtendWith(MockitoExtension.class)
class StockServerRedisTrailingStopResponseRepositoryTest {

    private static final String KEY = "user:trailing:stop:user";

    @InjectMocks private StockServerRedisTrailingStopResponseRepository sut;
    @Mock private RedisTemplate<String, StockServerTrailingStopResponseMessage> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private final StockServerTrailingStopResponseMessage message = new StockServerTrailingStopResponseMessage(
            1L, "user", "005930", TrailingStopType.SELL, LeverageRatio.SPOT, 10, 3.0, 70_000, 67_900, Instant.now());

    @BeforeEach
    void setUp() {
        willReturn(hashOperations).given(redisTemplate).opsForHash();
    }

    @DisplayName("저장: 사용자 키의 해시에 id를 필드로 저장한다")
    @Test
    void whenSaving_thenPutsHash() {
        sut.save(message);

        then(hashOperations).should().put(KEY, "1", message);
    }

    @DisplayName("갱신: 같은 필드를 새 값으로 덮어쓴다")
    @Test
    void whenUpdating_thenPutsHash() {
        sut.update("user", 1L, message);

        then(hashOperations).should().put(KEY, "1", message);
    }

    @DisplayName("삭제: 해당 필드를 삭제한다")
    @Test
    void whenDeleting_thenDeletesField() {
        sut.delete("user", 1L);

        then(hashOperations).should().delete(KEY, "1");
    }
}
