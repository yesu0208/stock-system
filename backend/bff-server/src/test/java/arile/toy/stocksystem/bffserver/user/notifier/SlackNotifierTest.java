package arile.toy.stocksystem.bffserver.user.notifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SlackNotifierTest {

    private static final String WEBHOOK = "https://hooks.slack.example/services/abc";

    private SlackAlertThrottle throttle;
    private MockRestServiceServer server;
    private SlackNotifier notifier;

    @BeforeEach
    void setUp() {
        throttle = mock(SlackAlertThrottle.class);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        notifier = new SlackNotifier(throttle);
        ReflectionTestUtils.setField(notifier, "restClient", builder.build());
        ReflectionTestUtils.setField(notifier, "webhookUrl", WEBHOOK);
    }

    @Nested
    @DisplayName("웹훅 비활성화")
    class Disabled {

        @ParameterizedTest(name = "webhookUrl=[{0}]")
        @NullSource
        @ValueSource(strings = {"", "   ", "test-slack-webhook-url"})
        @DisplayName("웹훅 URL이 없거나 test-로 시작하면 아무 알림도 보내지 않는다")
        void skipsAll(String webhookUrl) {
            ReflectionTestUtils.setField(notifier, "webhookUrl", webhookUrl);

            notifier.notifySignUp("user1", "닉네임", Instant.now());
            notifier.notifyServerError("/api/v1/orders", new IllegalStateException("boom"));

            server.verify();   // 기대한 요청이 없으므로, 요청이 하나라도 나갔다면 실패
            verifyNoInteractions(throttle);
        }
    }

    @Nested
    @DisplayName("회원가입 알림")
    class SignUp {

        @Test
        @DisplayName("아이디·닉네임·한국 시간 기준 가입 시각을 담아 웹훅으로 보낸다")
        void notifySignUp() {
            server.expect(requestTo(WEBHOOK))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.text", allOf(
                            containsString("새 회원가입"),
                            containsString("`user1`"),
                            containsString("`닉네임`"),
                            containsString("2026-09-01 09:00:00"))))
                    .andRespond(withSuccess());

            notifier.notifySignUp("user1", "닉네임", Instant.parse("2026-09-01T00:00:00Z"));

            server.verify();
        }
    }

    @Nested
    @DisplayName("서버 에러 알림")
    class ServerError {

        @Test
        @DisplayName("억제 대상이 아니면 경로·예외 타입·메시지를 담아 웹훅으로 보낸다")
        void notifyServerError() {
            given(throttle.shouldSend("IllegalStateException")).willReturn(true);
            server.expect(requestTo(WEBHOOK))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.text", allOf(
                            containsString("서버 에러 발생"),
                            containsString("`/api/v1/orders`"),
                            containsString("`IllegalStateException`"),
                            containsString("boom"))))
                    .andRespond(withSuccess());

            notifier.notifyServerError("/api/v1/orders", new IllegalStateException("boom"));

            server.verify();
        }

        @Test
        @DisplayName("1분 안에 같은 타입의 에러가 이미 알려졌으면 보내지 않는다")
        void throttled() {
            given(throttle.shouldSend("IllegalStateException")).willReturn(false);

            notifier.notifyServerError("/api/v1/orders", new IllegalStateException("boom"));

            server.verify();
        }
    }
}
