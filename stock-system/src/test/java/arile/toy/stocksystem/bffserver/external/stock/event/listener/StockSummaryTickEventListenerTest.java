package arile.toy.stocksystem.bffserver.external.stock.event.listener;

import arile.toy.stocksystem.bffserver.account.service.AccountPushService;
import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.bffserver.session.UserRedisSubscriptionRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSummaryTickEventListenerTest {

    @Mock
    private AccountPushService accountPushService;

    @Mock
    private UserRedisSubscriptionRegistry subscriptionManager;

    @InjectMocks
    private StockSummaryTickEventListener listener;

    @Test
    @DisplayName("StockSummaryTickEvent 수신 시 모든 연결된 사용자에 대해 push 호출 (ArgumentCaptor 사용)")
    void givenEvent_whenHandleAccountUpdate_thenPushCalledForAllUsers_captor() {
        // given
        Set<String> users = Set.of("user1", "user2", "user3");
        when(subscriptionManager.getAllConnectedUsernames()).thenReturn(users);

        StockSummaryTickEvent event = new StockSummaryTickEvent("005930");

        // when
        listener.handleAccountUpdate(event);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(accountPushService, times(users.size())).push(captor.capture());

        List<String> capturedUsernames = captor.getAllValues();
        assertThat(capturedUsernames).containsExactlyInAnyOrderElementsOf(users);
    }


    @Test
    @DisplayName("예외 발생 시에도 push 호출 시도 후 로그만 출력")
    void givenException_whenHandleAccountUpdate_thenLogsError() {
        when(subscriptionManager.getAllConnectedUsernames()).thenThrow(new RuntimeException("Redis error"));

        StockSummaryTickEvent event = new StockSummaryTickEvent("005930");

        // when
        listener.handleAccountUpdate(event);

        // then
        verifyNoInteractions(accountPushService);
    }
}
