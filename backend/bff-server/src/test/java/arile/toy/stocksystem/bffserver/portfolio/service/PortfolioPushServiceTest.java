package arile.toy.stocksystem.bffserver.portfolio.service;

import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioPushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private PortfolioCalculator portfolioCalculator;

    @InjectMocks
    private PortfolioPushService service;

    @Test
    @DisplayName("계산한 포트폴리오를 사용자 전용 채널(/user/sub/portfolio)로 보낸다")
    void push() {
        PortfolioResponse response = new PortfolioResponse("user1", 1_000_000L, 500_000L, 50.0, List.of());
        given(portfolioCalculator.calculate("user1")).willReturn(response);

        service.push("user1");

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/portfolio", response);
    }

    @Test
    @DisplayName("계좌가 아직 없는 사용자면 보내지 않고 조용히 건너뛴다")
    void noAccount_skipped() {
        given(portfolioCalculator.calculate("user1")).willThrow(new RedisAccountNotFoundException("no account"));

        assertThatCode(() -> service.push("user1")).doesNotThrowAnyException();

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("계산이 실패해도 예외를 던지지 않는다 (접속자 전원 푸시 중 다음 사용자가 영향받지 않도록)")
    void calculationFails_swallowed() {
        given(portfolioCalculator.calculate("user1")).willThrow(new IllegalStateException("broken snapshot"));

        assertThatCode(() -> service.push("user1")).doesNotThrowAnyException();

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("전송이 실패해도 예외를 던지지 않는다")
    void sendFails_swallowed() {
        PortfolioResponse response = new PortfolioResponse("user1", 1_000_000L, 500_000L, 50.0, List.of());
        given(portfolioCalculator.calculate("user1")).willReturn(response);
        willThrow(new MessageDeliveryException("closed"))
                .given(messagingTemplate).convertAndSendToUser("user1", "/sub/portfolio", response);

        assertThatCode(() -> service.push("user1")).doesNotThrowAnyException();
    }
}
