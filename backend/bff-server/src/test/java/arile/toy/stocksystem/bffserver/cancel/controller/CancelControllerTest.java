package arile.toy.stocksystem.bffserver.cancel.controller;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelRequest;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResponse;
import arile.toy.stocksystem.bffserver.cancel.service.CancelIngressService;
import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
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

@WebMvcTest(CancelController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class CancelControllerTest {

    private static final String URL = "/api/v1/cancels";
    private static final CancelRequest REQUEST = new CancelRequest(1L, "005930");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CancelIngressService cancelIngressService;
    @MockitoBean private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명을 함께 넘겨 취소를 접수하고 주문 ID·종목코드를 반환한다")
    void cancel() throws Exception {
        given(cancelIngressService.receive("user1", REQUEST)).willReturn(new CancelResponse(1L, "005930"));

        mockMvc.perform(post(URL).with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "stockCode": "005930"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.stockCode").value("005930"));

        verify(cancelIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문에 다른 username을 넣어도 무시하고, 인증된 사용자명으로 접수한다 (타인 주문 취소 방지)")
    void cancel_ignoresUsernameInBody() throws Exception {
        given(cancelIngressService.receive("user1", REQUEST)).willReturn(new CancelResponse(1L, "005930"));

        mockMvc.perform(post(URL).with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "stockCode": "005930", "username": "victim"}
                                """))
                .andExpect(status().isOk());

        verify(cancelIngressService).receive("user1", REQUEST);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"stockCode\": \"005930\"}",
            "{\"orderId\": 1}",
            "{\"orderId\": 1, \"stockCode\": \"\"}"
    })
    @DisplayName("주문 ID나 종목코드가 없으면 400을 반환하고 접수하지 않는다")
    void cancel_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cancelIngressService);
    }

    @Test
    @DisplayName("장이 닫힌 종목이면 MarketClosedException 상태 코드로 응답하고 접수하지 않는다")
    void cancel_marketClosed() throws Exception {
        given(bffServerMarketPhaseRegistry.isClosed("005930")).willReturn(true);
        MarketClosedException exception = new MarketClosedException();

        mockMvc.perform(post(URL).with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "stockCode": "005930"}
                                """))
                .andExpect(status().is(exception.getStatus().value()));

        verifyNoInteractions(cancelIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void cancel_unauthenticated() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "stockCode": "005930"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cancelIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void cancel_nullPrincipal() {
        CancelController controller = new CancelController(cancelIngressService, bffServerMarketPhaseRegistry);

        assertThat(controller.cancel(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(cancelIngressService);
    }
}
