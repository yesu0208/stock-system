package arile.toy.stocksystem.accountserver.leverage.publisher;

import arile.toy.stocksystem.accountserver.leverage.event.publisher.InterestAppliedPublisher;
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
class InterestAppliedPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private InterestAppliedPublisher publisher;

    @Test
    @DisplayName("이자 반영 채널로 INTEREST_APPLIED 메시지를 발행한다")
    void publish() {
        publisher.publish();

        verify(redisTemplate).convertAndSend("account:interest-applied", "INTEREST_APPLIED");
    }

    @Test
    @DisplayName("발행 중 예외가 나도 호출자에게 전파하지 않는다")
    void publish_whenRedisFails_swallowsException() {
        given(redisTemplate.convertAndSend("account:interest-applied", "INTEREST_APPLIED"))
                .willThrow(new RedisConnectionFailureException("connection refused"));

        assertThatCode(() -> publisher.publish()).doesNotThrowAnyException();
    }
}
