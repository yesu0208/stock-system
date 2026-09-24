package arile.toy.stocksystem.bffserver.account.event.subscriber;

import arile.toy.stocksystem.bffserver.account.service.AccountPushService;
import arile.toy.stocksystem.bffserver.portfolio.service.PortfolioPushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisAccountUpdateEventSubscriberTest {

    private final AccountPushService accountPushService = mock(AccountPushService.class);
    private final PortfolioPushService portfolioPushService = mock(PortfolioPushService.class);
    private final RedisAccountUpdateEventSubscriber subscriber =
            new RedisAccountUpdateEventSubscriber(new ObjectMapper(), accountPushService, portfolioPushService);

    private static Message message(String json) {
        return new DefaultMessage("account:update:user1".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("계좌 변경 이벤트의 사용자에게 계좌를 먼저, 이어서 포트폴리오를 푸시한다")
    void onMessage() {
        subscriber.onMessage(message("{\"username\": \"user1\"}"), null);

        InOrder inOrder = inOrder(accountPushService, portfolioPushService);
        inOrder.verify(accountPushService).push("user1");
        inOrder.verify(portfolioPushService).push("user1");
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("not json"), null)).doesNotThrowAnyException();

        verifyNoInteractions(accountPushService, portfolioPushService);
    }
}
