package arile.toy.stocksystem.bffserver.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRedisSubscriptionRegistryTest {

    @Mock
    private RedisMessageListenerContainer container;

    @InjectMocks
    private UserRedisSubscriptionRegistry registry;

    private final String sessionId = "sess1";
    private final String username = "user1";

    @BeforeEach
    void setup() {
    }

    @Test
    @DisplayName("신규 사용자 구독 시 모든 리스너 등록")
    void givenNewUser_whenSubscribe_thenRegistersAllSubscribers() {
        // given

        // when
        registry.subscribe(sessionId, username);

        // then
        Set<String> connected = registry.getAllConnectedUsernames();
        assertTrue(connected.contains(username));

        verify(container, times(6)).addMessageListener(any(), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("기존 사용자 구독 시 참조 카운트 증가")
    void givenExistingUser_whenSubscribe_thenIncrementsRefCount() {
        // given
        registry.subscribe(sessionId, username);
        String newSession = "sess2";

        // when
        registry.subscribe(newSession, username);

        // then
        verify(container, times(6)).addMessageListener(any(), any(ChannelTopic.class));

        Set<String> connected = registry.getAllConnectedUsernames();
        assertTrue(connected.contains(username));
    }

    @Test
    @DisplayName("단일 세션 disconnect 시 사용자 제거 및 리스너 삭제")
    void givenSingleSession_whenDisconnect_thenRemovesUserAndListeners() {
        // given
        registry.subscribe(sessionId, username);

        // when
        registry.disconnect(sessionId);

        // then
        verify(container, times(6)).removeMessageListener(any(), any(ChannelTopic.class));

        Set<String> connected = registry.getAllConnectedUsernames();
        assertFalse(connected.contains(username));
    }

    @Test
    @DisplayName("다중 세션 disconnect 시 참조 카운트 감소 후 마지막에 리스너 삭제")
    void givenMultipleSessions_whenDisconnect_thenDecrementsRefCountAndRemovesListenersAtLast() {
        // given
        registry.subscribe(sessionId, username);
        String secondSession = "sess2";
        registry.subscribe(secondSession, username);

        // when
        registry.disconnect(sessionId);

        // then
        verify(container, times(0)).removeMessageListener(any(), any(ChannelTopic.class));

        Set<String> connected = registry.getAllConnectedUsernames();
        assertTrue(connected.contains(username));

        // when
        registry.disconnect(secondSession);

        // then
        verify(container, times(6)).removeMessageListener(any(), any(ChannelTopic.class));
        connected = registry.getAllConnectedUsernames();
        assertFalse(connected.contains(username));
    }

    @Test
    @DisplayName("존재하지 않는 세션 disconnect 시 아무 동작도 하지 않음")
    void givenUnknownSession_whenDisconnect_thenDoesNothing() {
        // when
        registry.disconnect("unknown");

        // then
        verify(container, never()).removeMessageListener(any(), any(ChannelTopic.class));
    }
}
