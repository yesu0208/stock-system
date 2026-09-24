package arile.toy.stocksystem.bffserver.alert.controller;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertRequest;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponse;
import arile.toy.stocksystem.bffserver.alert.service.AlertIngressService;
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

@WebMvcTest(AlertController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class AlertControllerTest {

    private static final String URL = "/api/v1/alerts";
    private static final String BODY = """
            {"stockCode": "005930", "direction": "ABOVE", "triggerPrice": 70000}
            """;
    private static final AlertRequest REQUEST = new AlertRequest("005930", AlertDirection.ABOVE, 70_000);
    private static final AlertResponse RESPONSE = new AlertResponse("user1", "005930", AlertDirection.ABOVE, 70_000);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AlertIngressService alertIngressService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명으로 알림 등록을 접수하고 접수 내용을 반환한다 (장 운영 여부와 무관)")
    void register() throws Exception {
        given(alertIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.direction").value("ABOVE"))
                .andExpect(jsonPath("$.triggerPrice").value(70_000));

        verify(alertIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문의 username은 무시하고 인증된 사용자명으로 접수한다")
    void register_ignoresUsernameInBody() throws Exception {
        given(alertIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "direction": "ABOVE", "triggerPrice": 70000, "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(alertIngressService).receive("user1", REQUEST);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"direction\": \"ABOVE\", \"triggerPrice\": 70000}",
            "{\"stockCode\": \"\", \"direction\": \"ABOVE\", \"triggerPrice\": 70000}",
            "{\"stockCode\": \"005930\", \"triggerPrice\": 70000}",
            "{\"stockCode\": \"005930\", \"direction\": \"ABOVE\"}",
            "{\"stockCode\": \"005930\", \"direction\": \"ABOVE\", \"triggerPrice\": 0}",
            "{\"stockCode\": \"005930\", \"direction\": \"SIDEWAYS\", \"triggerPrice\": 70000}"
    })
    @DisplayName("필수값이 없거나, 발동 가격이 0 이하이거나, 알 수 없는 방향이면 400을 반환한다")
    void register_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(alertIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void register_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alertIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void register_nullPrincipal() {
        AlertController controller = new AlertController(alertIngressService);

        assertThat(controller.register(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(alertIngressService);
    }
}
