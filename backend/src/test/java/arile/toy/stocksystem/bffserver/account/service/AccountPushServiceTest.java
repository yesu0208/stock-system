package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountPushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AccountCalculator calculator;

    @InjectMocks
    private AccountPushService accountPushService;

    @Test
    @DisplayName("계좌 푸시 시 계산 후 메시지 전송")
    void givenUsername_whenPush_thenCalculatorCalledAndMessageSent() {
        String username = "user1";

        // given
        AccountResponse response = mock(AccountResponse.class);
        when(calculator.calculate(username)).thenReturn(response);

        // when
        accountPushService.push(username);

        // then
        verify(calculator).calculate(username);
        verify(messagingTemplate).convertAndSendToUser(
                eq(username),
                eq("/sub/account"),
                eq(response)
        );
    }
}
