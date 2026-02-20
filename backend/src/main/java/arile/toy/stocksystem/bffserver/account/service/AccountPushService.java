package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AccountCalculator calculator;

    public void push(String username) {
        AccountResponse response = calculator.calculate(username);
        messagingTemplate.convertAndSendToUser(
                username,
                "/sub/account",
                response
        );
    }
}
