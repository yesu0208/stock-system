package arile.toy.stocksystem.bffserver.external.stock.event.listener;

import arile.toy.stocksystem.bffserver.account.service.AccountPushService;
import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.bffserver.portfolio.service.PortfolioPushService;
import arile.toy.stocksystem.bffserver.session.UserRedisSubscriptionRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockSummaryTickEventListenerTest {

    private static final StockSummaryTickEvent EVENT = new StockSummaryTickEvent("005930");

    @Mock private AccountPushService accountPushService;
    @Mock private PortfolioPushService portfolioPushService;
    @Mock private UserRedisSubscriptionRegistry subscriptionManager;

    @InjectMocks
    private StockSummaryTickEventListener listener;

    @Test
    @DisplayName("접속자마다 계좌를 먼저, 이어서 포트폴리오를 푸시한다")
    void pushesToAllConnectedUsers() {
        Set<String> users = new LinkedHashSet<>(List.of("user1", "user2"));
        given(subscriptionManager.getAllConnectedUsernames()).willReturn(users);

        listener.handleAccountUpdate(EVENT);

        InOrder inOrder = inOrder(accountPushService, portfolioPushService);
        inOrder.verify(accountPushService).push("user1");
        inOrder.verify(portfolioPushService).push("user1");
        inOrder.verify(accountPushService).push("user2");
        inOrder.verify(portfolioPushService).push("user2");
    }

    @Test
    @DisplayName("접속자가 없으면 아무것도 푸시하지 않는다")
    void noConnectedUsers() {
        given(subscriptionManager.getAllConnectedUsernames()).willReturn(Set.of());

        listener.handleAccountUpdate(EVENT);

        verifyNoInteractions(accountPushService, portfolioPushService);
    }

    @Test
    @DisplayName("접속자 목록 조회가 실패해도 예외를 던지지 않는다 (다른 이벤트 리스너에 영향 없음)")
    void registryFails_swallowed() {
        given(subscriptionManager.getAllConnectedUsernames()).willThrow(new IllegalStateException("boom"));

        assertThatCode(() -> listener.handleAccountUpdate(EVENT)).doesNotThrowAnyException();

        verifyNoInteractions(accountPushService, portfolioPushService);
    }
}
