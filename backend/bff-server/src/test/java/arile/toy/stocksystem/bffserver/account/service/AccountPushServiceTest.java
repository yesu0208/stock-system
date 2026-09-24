package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountPushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private AccountCalculator calculator;

    @InjectMocks
    private AccountPushService service;

    @Test
    @DisplayName("계산한 계좌 정보를 사용자 전용 /sub/account 채널로 보낸다")
    void push() {
        AccountResponse response = mock(AccountResponse.class);
        given(calculator.calculate("user1")).willReturn(response);

        service.push("user1");

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/account", response);
    }

    @Test
    @DisplayName("계좌 데이터가 없으면 보내지 않고 예외도 던지지 않는다 (예: 회원가입을 거치지 않은 관리자 계정)")
    void accountNotFound_skips() {
        given(calculator.calculate("admin")).willThrow(mock(RedisAccountNotFoundException.class));

        assertThatCode(() -> service.push("admin")).doesNotThrowAnyException();

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("그 밖의 계산 오류도 예외를 던지지 않고 건너뛴다")
    void unexpectedError_skips() {
        given(calculator.calculate("user1")).willThrow(new IllegalStateException("boom"));

        assertThatCode(() -> service.push("user1")).doesNotThrowAnyException();

        verifyNoInteractions(messagingTemplate);
    }
}
