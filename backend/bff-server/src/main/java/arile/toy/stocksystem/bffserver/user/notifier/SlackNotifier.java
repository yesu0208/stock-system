package arile.toy.stocksystem.bffserver.user.notifier;

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
public class SlackNotifier {

    private final RestClient restClient = RestClient.create();

    @Value("${slack.webhook-url}")
    private String webhookUrl;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    public void notifySignUp(String username, String nickname, Instant createdDateTime) {
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.startsWith("test-")) {
            log.debug("Slack webhook 비활성화 상태(test 프로필 등). 알림 생략. username={}", username);
            return;
        }

        String text = """
                :tada: *새 회원가입*
                • 아이디: `%s`
                • 닉네임: `%s`
                • 가입시각: %s
                """.formatted(username, nickname, FORMATTER.format(createdDateTime));

        restClient.post()
                .uri(webhookUrl)
                .body(Map.of("text", text))
                .retrieve()
                .toBodilessEntity();
    }
}
