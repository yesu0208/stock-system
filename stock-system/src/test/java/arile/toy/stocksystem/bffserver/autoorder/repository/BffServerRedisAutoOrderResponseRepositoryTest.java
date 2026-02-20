package arile.toy.stocksystem.bffserver.autoorder.repository;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BffServerRedisAutoOrderResponseRepositoryTest {

    @Mock
    private RedisTemplate<String, AutoOrderResponseMessage> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private BffServerRedisAutoOrderResponseRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("사용자별 AutoOrderResponseMessage 모두 조회")
    void givenExistingAutoOrders_whenFindAll_thenReturnAllMessages() {
        // given
        String username = "testUser";
        AutoOrderResponseMessage msg1 = new AutoOrderResponseMessage(1L, username, "005930",
                AutoOrderType.BUY, 50000, 50000, 50, Instant.now());
        AutoOrderResponseMessage msg2 = new AutoOrderResponseMessage(2L, username, "005930",
                AutoOrderType.BUY, 60000, 60000, 60, Instant.now());

        when(hashOperations.values("user:auto:order:" + username))
                .thenReturn(List.of(msg1, msg2));

        // when
        List<AutoOrderResponseMessage> result = repository.findAll(username);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(msg1));
        assertTrue(result.contains(msg2));
        verify(hashOperations).values("user:auto:order:" + username);
    }

    @Test
    @DisplayName("사용자별 AutoOrderResponseMessage가 없으면 빈 리스트 반환")
    void givenNoAutoOrders_whenFindAll_thenReturnEmptyList() {
        // given
        String username = "emptyUser";
        when(hashOperations.values("user:auto:order:" + username))
                .thenReturn(List.of());

        // when
        List<AutoOrderResponseMessage> result = repository.findAll(username);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(hashOperations).values("user:auto:order:" + username);
    }
}
