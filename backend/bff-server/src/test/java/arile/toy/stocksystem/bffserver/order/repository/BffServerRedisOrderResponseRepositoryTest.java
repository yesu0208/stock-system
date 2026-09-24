package arile.toy.stocksystem.bffserver.order.repository;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BffServerRedisOrderResponseRepositoryTest {

    @Test
    @DisplayName("user:order:{사용자} 해시에 저장된 미체결 주문을 모두 조회하고, 없으면 빈 목록을 반환한다")
    @SuppressWarnings("unchecked")
    void findAll() {
        RedisTemplate<String, OrderResponseMessage> redisTemplate = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();
        OrderResponseMessage first = mock(OrderResponseMessage.class);
        OrderResponseMessage second = mock(OrderResponseMessage.class);
        given(hashOps.values("user:order:user1")).willReturn(List.of(first, second));
        given(hashOps.values("user:order:user2")).willReturn(List.of());

        BffServerRedisOrderResponseRepository repository = new BffServerRedisOrderResponseRepository(redisTemplate);

        assertThat(repository.findAll("user1")).containsExactly(first, second);
        assertThat(repository.findAll("user2")).isEmpty();
    }
}
