package arile.toy.stocksystem.bffserver.trailingstop.repository;

import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BffServerRedisTrailingStopResponseRepositoryTest {

    @Test
    @DisplayName("user:trailing:stop:{사용자} 해시에 저장된 트레일링 스탑을 모두 조회하고, 없으면 빈 목록을 반환한다")
    @SuppressWarnings("unchecked")
    void findAll() {
        RedisTemplate<String, TrailingStopResponseMessage> redisTemplate = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();
        TrailingStopResponseMessage first = mock(TrailingStopResponseMessage.class);
        given(hashOps.values("user:trailing:stop:user1")).willReturn(List.of(first));
        given(hashOps.values("user:trailing:stop:user2")).willReturn(List.of());

        BffServerRedisTrailingStopResponseRepository repository =
                new BffServerRedisTrailingStopResponseRepository(redisTemplate);

        assertThat(repository.findAll("user1")).containsExactly(first);
        assertThat(repository.findAll("user2")).isEmpty();
    }
}
