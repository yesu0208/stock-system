package arile.toy.stocksystem.bffserver.alertcancel.controller;

import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelRequest;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResponse;
import arile.toy.stocksystem.bffserver.alertcancel.service.AlertCancelIngressService;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertCancelController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class AlertCancelControllerTest {

    private static final String URL = "/api/v1/alerts/cancel";
    private static final String BODY = """
            {"alertId": 1, "stockCode": "005930"}
            """;
    private static final AlertCancelRequest REQUEST = new AlertCancelRequest(1L, "005930");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AlertCancelIngressService alertCancelIngressService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명을 함께 넘겨 알림 취소를 접수한다 (장 운영 여부와 무관)")
    void cancel() throws Exception {
        given(alertCancelIngressService.receive("user1", REQUEST)).willReturn(new AlertCancelResponse(1L, "005930"));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(1))
                .andExpect(jsonPath("$.stockCode").value("005930"));

        verify(alertCancelIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문의 username은 무시하고 인증된 사용자명으로 접수한다")
    void cancel_ignoresUsernameInBody() throws Exception {
        given(alertCancelIngressService.receive("user1", REQUEST)).willReturn(new AlertCancelResponse(1L, "005930"));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"alertId": 1, "stockCode": "005930", "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(alertCancelIngressService).receive("user1", REQUEST);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"stockCode\": \"005930\"}",
            "{\"alertId\": 1}",
            "{\"alertId\": 1, \"stockCode\": \"\"}"
    })
    @DisplayName("알림 ID나 종목코드가 없으면 400을 반환한다")
    void cancel_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(alertCancelIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void cancel_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alertCancelIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void cancel_nullPrincipal() {
        AlertCancelController controller = new AlertCancelController(alertCancelIngressService);

        assertThat(controller.cancel(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
