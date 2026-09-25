package arile.toy.stocksystem.stockserver.alert.repository;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;
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

@DisplayName("[Repository] 알림 응답 Redis 저장소 테스트")
@ExtendWith(MockitoExtension.class)
class StockServerRedisAlertResponseRepositoryTest {

    @InjectMocks private StockServerRedisAlertResponseRepository sut;
    @Mock private RedisTemplate<String, StockServerAlertResponseMessage> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    void setUp() {
        willReturn(hashOperations).given(redisTemplate).opsForHash();
    }

    @DisplayName("저장은 사용자 키 해시에 id 필드로 넣고, 삭제는 해당 필드를 지운다")
    @Test
    void whenSavingAndDeleting_thenUsesHashField() {
        var message = StockServerAlertResponseMessage.of(1L, "user", "005930", AlertDirection.ABOVE, 72_000, Instant.now());

        sut.save(message);
        sut.delete("user", 1L);

        then(hashOperations).should().put("user:alert:user", "1", message);
        then(hashOperations).should().delete("user:alert:user", "1");
    }
}
