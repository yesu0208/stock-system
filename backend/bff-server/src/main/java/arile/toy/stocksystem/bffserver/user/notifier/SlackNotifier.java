package arile.toy.stocksystem.bffserver.user.notifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotifier {

    private final RestClient restClient = RestClient.create();
    private final SlackAlertThrottle throttle;

    @Value("${slack.webhook-url}")
    private String webhookUrl;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    private boolean isDisabled() {
        return webhookUrl == null || webhookUrl.isBlank() || webhookUrl.startsWith("test-");
    }

    public void notifySignUp(String username, String nickname, Instant createdDateTime) {
        if (isDisabled()) {
            log.debug("Slack webhook 비활성화 상태(test 프로필 등). 알림 생략. username={}", username);
            return;
        }

        String text = """
                :tada: *새 회원가입*
                • 아이디: `%s`
                • 닉네임: `%s`
                • 가입시각: %s
                """.formatted(username, nickname, FORMATTER.format(createdDateTime));

        send(text);
    }

    public void notifyServerError(String path, Exception exception) {
        if (isDisabled()) return;

        String errorKey = exception.getClass().getSimpleName();
        if (!throttle.shouldSend(errorKey)) {
            log.debug("Slack 알림 억제됨 (1분 내 동일 에러). errorType={}", errorKey);
            return;
        }

        String text = """
                :rotating_light: *서버 에러 발생*
                • 경로: `%s`
                • 타입: `%s`
                • 메시지: %s
                """.formatted(path, errorKey, exception.getMessage());

        send(text);
    }

    private void send(String text) {
        restClient.post()
                .uri(webhookUrl)
                .body(Map.of("text", text))
                .retrieve()
                .toBodilessEntity();
    }
}
