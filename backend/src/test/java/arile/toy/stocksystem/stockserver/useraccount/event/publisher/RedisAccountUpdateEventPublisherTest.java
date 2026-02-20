package arile.toy.stocksystem.stockserver.useraccount.event.publisher;

import arile.toy.stocksystem.stockserver.useraccount.event.AccountUpdateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAccountUpdateEventPublisherTest {

    @Mock
    private RedisTemplate<String, AccountUpdateEvent> redisTemplate;

    @InjectMocks
    private RedisAccountUpdateEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<AccountUpdateEvent> eventCaptor;

    @Test
    @DisplayName("AccountUpdate 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenUsername_whenPublish_thenSendAccountUpdateEvent() {

        // given
        String username = "user1";
        AccountUpdateEvent event = AccountUpdateEvent.of(username);

        // when
        publisher.publish(username);

        // then
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), eventCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("user:account.user1:event");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenRedisThrowsException_whenPublish_thenDoNotThrow() {

        String username = "user1";

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        assertDoesNotThrow(() -> publisher.publish(username));
    }
}
