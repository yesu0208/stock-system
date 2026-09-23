package arile.toy.stocksystem.bffserver.trailingstop.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.exception.leverage.LeverageNotAllowedException;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.leverage.service.LeverageAccessValidator;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopRequest;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponse;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstop.service.TrailingStopIngressService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrailingStopController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class TrailingStopControllerTest {

    private static final String URL = "/api/v1/trailing-stops";
    private static final String BODY = """
            {"stockCode": "005930", "trailingStopType": "SELL", "orderQuantity": 10, "stopPercent": 3.0, "basePrice": 70000}
            """;
    private static final TrailingStopRequest REQUEST =
            new TrailingStopRequest("005930", TrailingStopType.SELL, 10, 3.0, 70_000, null);
    private static final TrailingStopResponse RESPONSE =
            new TrailingStopResponse("user1", "005930", TrailingStopType.SELL, 10, 3.0, 70_000, LeverageRatio.SPOT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TrailingStopIngressService trailingStopIngressService;
    @MockitoBean private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    @MockitoBean private LeverageAccessValidator leverageAccessValidator;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    private static String bodyWithStopPercent(String stopPercent) {
        return """
                {"stockCode": "005930", "trailingStopType": "SELL", "orderQuantity": 10, "stopPercent": %s, "basePrice": 70000}
                """.formatted(stopPercent);
    }

    @Test
    @DisplayName("로그인한 사용자명으로 레버리지 자격을 검증한 뒤 트레일링 스탑을 접수한다")
    void trailingStop() throws Exception {
        given(trailingStopIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.stopPercent").value(3.0))
                .andExpect(jsonPath("$.basePrice").value(70_000));

        verify(leverageAccessValidator).validate("user1", LeverageRatio.SPOT);
        verify(trailingStopIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문의 username은 무시하고 인증된 사용자명으로 접수한다")
    void trailingStop_ignoresUsernameInBody() throws Exception {
        given(trailingStopIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "trailingStopType": "SELL", "orderQuantity": 10, "stopPercent": 3.0,
                         "basePrice": 70000, "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(trailingStopIngressService).receive("user1", REQUEST);
    }

    @ParameterizedTest(name = "stopPercent={0}")
    @ValueSource(strings = {"0.1", "30.0"})
    @DisplayName("추적 폭 경계값(0.1%, 30%)은 허용한다")
    void trailingStop_stopPercentBoundary_allowed(String stopPercent) throws Exception {
        given(trailingStopIngressService.receive(eq("user1"), any())).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithStopPercent(stopPercent)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "stopPercent={0}")
    @ValueSource(strings = {"0.09", "30.1", "100", "150"})
    @DisplayName("추적 폭이 0.1% 미만이거나 30% 초과면 400을 반환한다 (SELL 발동가 0 이하 방지)")
    void trailingStop_stopPercentOutOfRange(String stopPercent) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithStopPercent(stopPercent)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(leverageAccessValidator, trailingStopIngressService);
    }

    @Test
    @DisplayName("레버리지 등급이 부족하면 400을 반환하고 접수하지 않는다")
    void trailingStop_leverageNotAllowed() throws Exception {
        willThrow(new LeverageNotAllowedException("GOLD"))
                .given(leverageAccessValidator).validate("user1", LeverageRatio.X2);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "trailingStopType": "SELL", "orderQuantity": 10, "stopPercent": 3.0,
                         "basePrice": 70000, "leverageRatio": "X2"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trailingStopIngressService);
    }

    @Test
    @DisplayName("장이 닫힌 종목이면 등급 검증 전에 거부한다")
    void trailingStop_marketClosed() throws Exception {
        given(bffServerMarketPhaseRegistry.isClosed("005930")).willReturn(true);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(new MarketClosedException().getStatus().value()));

        verifyNoInteractions(leverageAccessValidator, trailingStopIngressService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"trailingStopType\": \"SELL\", \"orderQuantity\": 10, \"stopPercent\": 3.0, \"basePrice\": 70000}",
            "{\"stockCode\": \"005930\", \"orderQuantity\": 10, \"stopPercent\": 3.0, \"basePrice\": 70000}",
            "{\"stockCode\": \"005930\", \"trailingStopType\": \"SELL\", \"orderQuantity\": 0, \"stopPercent\": 3.0, \"basePrice\": 70000}",
            "{\"stockCode\": \"005930\", \"trailingStopType\": \"SELL\", \"orderQuantity\": 10, \"basePrice\": 70000}",
            "{\"stockCode\": \"005930\", \"trailingStopType\": \"SELL\", \"orderQuantity\": 10, \"stopPercent\": 3.0, \"basePrice\": 0}"
    })
    @DisplayName("필수값이 없거나 수량·기준가가 0 이하면 400을 반환한다")
    void trailingStop_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(leverageAccessValidator, trailingStopIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void trailingStop_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trailingStopIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void trailingStop_nullPrincipal() {
        TrailingStopController controller = new TrailingStopController(
                trailingStopIngressService, bffServerMarketPhaseRegistry, leverageAccessValidator);

        assertThat(controller.trailingStop(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
