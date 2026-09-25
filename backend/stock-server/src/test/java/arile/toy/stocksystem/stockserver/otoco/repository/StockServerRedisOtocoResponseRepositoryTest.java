package arile.toy.stocksystem.stockserver.otoco.repository;

import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.BDDMockito.*;

@DisplayName("[Repository] OTOCO 응답 Redis 저장소 테스트")
@ExtendWith(MockitoExtension.class)
class StockServerRedisOtocoResponseRepositoryTest {

    private static final String KEY = "user:otoco:user";

    @InjectMocks private StockServerRedisOtocoResponseRepository sut;
    @Mock private RedisTemplate<String, StockServerOtocoResponseMessage> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private final StockServerOtocoResponseMessage message =
            StockServerOtocoResponseMessage.fromEntity(OtocoFixtures.entity(OtocoStatus.WAITING_ENTRY));

    @BeforeEach
    void setUp() {
        willReturn(hashOperations).given(redisTemplate).opsForHash();
    }

    @DisplayName("저장·갱신은 사용자 키 해시에 id 필드로 넣고, 삭제는 해당 필드를 지운다")
    @Test
    void whenSavingUpdatingDeleting_thenUsesHashField() {
        sut.save(message);
        sut.update("user", 1L, message);
        sut.delete("user", 1L);

        then(hashOperations).should(times(2)).put(KEY, "1", message);
        then(hashOperations).should().delete(KEY, "1");
    }
}
