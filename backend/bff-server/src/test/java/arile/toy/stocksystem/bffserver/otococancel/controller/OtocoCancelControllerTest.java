package arile.toy.stocksystem.bffserver.otococancel.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelRequest;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelResponse;
import arile.toy.stocksystem.bffserver.otococancel.service.OtocoCancelIngressService;
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

@WebMvcTest(OtocoCancelController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class OtocoCancelControllerTest {

    private static final String URL = "/api/v1/otocos/cancel";
    private static final String BODY = """
            {"otocoId": 1, "stockCode": "005930"}
            """;
    private static final OtocoCancelRequest REQUEST = new OtocoCancelRequest(1L, "005930");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private OtocoCancelIngressService otocoCancelIngressService;
    @MockitoBean private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명을 함께 넘겨 OTOCO 취소를 접수한다")
    void cancel() throws Exception {
        given(otocoCancelIngressService.receive("user1", REQUEST)).willReturn(new OtocoCancelResponse(1L, "005930"));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otocoId").value(1))
                .andExpect(jsonPath("$.stockCode").value("005930"));

        verify(otocoCancelIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문의 username은 무시하고 인증된 사용자명으로 접수한다")
    void cancel_ignoresUsernameInBody() throws Exception {
        given(otocoCancelIngressService.receive("user1", REQUEST)).willReturn(new OtocoCancelResponse(1L, "005930"));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"otocoId": 1, "stockCode": "005930", "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(otocoCancelIngressService).receive("user1", REQUEST);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"stockCode\": \"005930\"}",
            "{\"otocoId\": 1}",
            "{\"otocoId\": 1, \"stockCode\": \"\"}"
    })
    @DisplayName("OTOCO ID나 종목코드가 없으면 400을 반환한다")
    void cancel_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(otocoCancelIngressService);
    }

    @Test
    @DisplayName("장이 닫힌 종목이면 접수하지 않는다")
    void cancel_marketClosed() throws Exception {
        given(bffServerMarketPhaseRegistry.isClosed("005930")).willReturn(true);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(new MarketClosedException().getStatus().value()));

        verifyNoInteractions(otocoCancelIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void cancel_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(otocoCancelIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void cancel_nullPrincipal() {
        OtocoCancelController controller =
                new OtocoCancelController(otocoCancelIngressService, bffServerMarketPhaseRegistry);

        assertThat(controller.cancel(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
