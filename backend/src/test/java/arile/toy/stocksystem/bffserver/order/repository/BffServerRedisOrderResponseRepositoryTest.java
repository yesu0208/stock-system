package arile.toy.stocksystem.bffserver.order.repository;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
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
class BffServerRedisOrderResponseRepositoryTest {

    @Mock
    private RedisTemplate<String, OrderResponseMessage> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private BffServerRedisOrderResponseRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("기존 사용자 조회 시 주문 응답 리스트 반환")
    void givenExistingUsername_whenFindAll_thenReturnsOrderResponseList() {
        // given
        String username = "testUser";
        OrderResponseMessage msg1 = new OrderResponseMessage(
                1L, username, "005930", OrderType.BUY, 50000,
                500, 500, Instant.now()
        );
        OrderResponseMessage msg2 = new OrderResponseMessage(
                2L, username, "005930", OrderType.SELL, 60000,
                600, 600, Instant.now()
        );

        when(hashOperations.values("user:order:" + username))
                .thenReturn(List.of(msg1, msg2));

        // when
        List<OrderResponseMessage> result = repository.findAll(username);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(msg1));
        assertTrue(result.contains(msg2));
        verify(hashOperations).values("user:order:" + username);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 빈 리스트 반환")
    void givenNonExistingUsername_whenFindAll_thenReturnsEmptyList() {
        // given
        String username = "emptyUser";
        when(hashOperations.values("user:order:" + username))
                .thenReturn(List.of());

        // when
        List<OrderResponseMessage> result = repository.findAll(username);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(hashOperations).values("user:order:" + username);
    }
}
