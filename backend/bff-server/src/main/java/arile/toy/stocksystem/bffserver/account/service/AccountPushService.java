package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
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

    /**
     * 사용자 1명의 계좌 정보를 계산해 푸시한
     * 실패해도 예외를 던지지 않음: 호출자(시세 변경 리스너)는 접속자 전원을 순회하므로,
     * 한 명의 실패가 뒤이은 포트폴리오 푸시나 다른 사용자의 푸시를 막지 않도록 여기서 끝냄.
     */
    public void push(String username) {
        try {
            AccountResponse response = calculator.calculate(username);
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/sub/account",
                    response
            );
        } catch (RedisAccountNotFoundException e) {
            log.debug("No account data found for username={}. Skip account push.", username);
        } catch (Exception e) {
            log.error("Failed to push account for username={}", username, e);
        }
    }
}
