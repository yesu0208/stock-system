package arile.toy.stocksystem.accountserver.useraccount.event.publisher;

import arile.toy.stocksystem.accountserver.useraccount.event.AccountUpdateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisAccountUpdateEventPublisherTest {

    private static final String USERNAME = "user1";

    @Mock
    private RedisTemplate<String, AccountUpdateEvent> accountUpdateEventRedisTemplate;

    @InjectMocks
    private RedisAccountUpdateEventPublisher publisher;

    @Test
    @DisplayName("사용자별 채널로 계좌 업데이트 이벤트를 발행한다")
    void publish() {
        publisher.publish(USERNAME);

        verify(accountUpdateEventRedisTemplate).convertAndSend(
                "user:account.user1:event",
                AccountUpdateEvent.of(USERNAME)
        );
    }

    @Test
    @DisplayName("Redis 발행 중 예외가 나도 호출자에게 전파하지 않는다")
    void publish_whenRedisFails_swallowsException() {
        given(accountUpdateEventRedisTemplate.convertAndSend(anyString(), any(AccountUpdateEvent.class)))
                .willThrow(new RedisConnectionFailureException("connection refused"));

        assertThatCode(() -> publisher.publish(USERNAME)).doesNotThrowAnyException();
    }
}
