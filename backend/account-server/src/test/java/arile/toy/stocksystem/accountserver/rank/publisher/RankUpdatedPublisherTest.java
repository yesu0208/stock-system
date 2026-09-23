package arile.toy.stocksystem.accountserver.rank.publisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RankUpdatedPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RankUpdatedPublisher publisher;

    @Test
    @DisplayName("랭크 갱신 채널로 RANK_UPDATED 메시지를 발행한다")
    void publish() {
        publisher.publish();

        verify(redisTemplate).convertAndSend("account:rank-updated", "RANK_UPDATED");
    }

    @Test
    @DisplayName("발행 중 예외가 나도 호출자에게 전파하지 않는다")
    void publish_whenRedisFails_swallowsException() {
        given(redisTemplate.convertAndSend("account:rank-updated", "RANK_UPDATED"))
                .willThrow(new RedisConnectionFailureException("connection refused"));

        assertThatCode(() -> publisher.publish()).doesNotThrowAnyException();
    }
}
