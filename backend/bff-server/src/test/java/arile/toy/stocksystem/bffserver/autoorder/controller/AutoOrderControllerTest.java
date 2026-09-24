package arile.toy.stocksystem.bffserver.autoorder.controller;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderRequest;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponse;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.service.AutoOrderIngressService;
import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.exception.leverage.LeverageNotAllowedException;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.leverage.service.LeverageAccessValidator;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutoOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class AutoOrderControllerTest {

    private static final String URL = "/api/v1/auto-orders";
    private static final String BODY = """
            {"stockCode": "005930", "autoOrderType": "BUY", "triggerPrice": 68000, "orderPrice": 69000, "orderQuantity": 10}
            """;
    private static final AutoOrderRequest REQUEST =
            new AutoOrderRequest("005930", AutoOrderType.BUY, 68_000, 69_000, 10, null);
    private static final AutoOrderResponse RESPONSE =
            new AutoOrderResponse("user1", "005930", AutoOrderType.BUY, 68_000, 69_000, 10, LeverageRatio.SPOT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AutoOrderIngressService autoOrderIngressService;
    @MockitoBean private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    @MockitoBean private LeverageAccessValidator leverageAccessValidator;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명으로 레버리지 자격을 검증한 뒤 자동 주문을 접수한다")
    void autoOrder() throws Exception {
        given(autoOrderIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.triggerPrice").value(68_000))
                .andExpect(jsonPath("$.orderPrice").value(69_000));

        verify(leverageAccessValidator).validate("user1", LeverageRatio.SPOT);
        verify(autoOrderIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문의 username은 무시하고 인증된 사용자명으로 접수한다")
    void autoOrder_ignoresUsernameInBody() throws Exception {
        given(autoOrderIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "autoOrderType": "BUY", "triggerPrice": 68000, "orderPrice": 69000,
                         "orderQuantity": 10, "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(autoOrderIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("레버리지 등급이 부족하면 400을 반환하고 접수하지 않는다")
    void autoOrder_leverageNotAllowed() throws Exception {
        willThrow(new LeverageNotAllowedException("PLATINUM"))
                .given(leverageAccessValidator).validate("user1", LeverageRatio.X2_5);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "autoOrderType": "BUY", "triggerPrice": 68000, "orderPrice": 69000,
                         "orderQuantity": 10, "leverageRatio": "X2_5"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autoOrderIngressService);
    }

    @Test
    @DisplayName("장이 닫힌 종목이면 등급 검증 전에 거부한다")
    void autoOrder_marketClosed() throws Exception {
        given(bffServerMarketPhaseRegistry.isClosed("005930")).willReturn(true);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(new MarketClosedException().getStatus().value()));

        verifyNoInteractions(leverageAccessValidator, autoOrderIngressService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"autoOrderType\": \"BUY\", \"triggerPrice\": 68000, \"orderPrice\": 69000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"triggerPrice\": 68000, \"orderPrice\": 69000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"autoOrderType\": \"BUY\", \"orderPrice\": 69000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"autoOrderType\": \"BUY\", \"triggerPrice\": 0, \"orderPrice\": 69000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"autoOrderType\": \"BUY\", \"triggerPrice\": 68000, \"orderPrice\": -1, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"autoOrderType\": \"BUY\", \"triggerPrice\": 68000, \"orderPrice\": 69000, \"orderQuantity\": 0}"
    })
    @DisplayName("필수값이 없거나 발동가·주문가·수량이 0 이하면 400을 반환한다")
    void autoOrder_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(leverageAccessValidator, autoOrderIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void autoOrder_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(autoOrderIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void autoOrder_nullPrincipal() {
        AutoOrderController controller =
                new AutoOrderController(autoOrderIngressService, bffServerMarketPhaseRegistry, leverageAccessValidator);

        assertThat(controller.autoOrder(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
