package arile.toy.stocksystem.bffserver.order.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.exception.leverage.LeverageNotAllowedException;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.leverage.service.LeverageAccessValidator;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.order.dto.OrderRequest;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.service.OrderIngressService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class OrderControllerTest {

    private static final String URL = "/api/v1/orders";
    private static final String BODY = """
            {"stockCode": "005930", "orderType": "BUY", "orderPrice": 70000, "orderQuantity": 10}
            """;
    private static final OrderRequest REQUEST =
            new OrderRequest("005930", OrderType.BUY, 70_000, 10, null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private OrderIngressService orderIngressService;
    @MockitoBean private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    @MockitoBean private LeverageAccessValidator leverageAccessValidator;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명으로 레버리지 자격을 검증한 뒤 주문을 접수하고 접수 내용을 반환한다")
    void order() throws Exception {
        given(orderIngressService.receive("user1", REQUEST)).willReturn(
                new OrderResponse("user1", "005930", OrderType.BUY, 70_000, 10, LeverageRatio.SPOT));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.orderType").value("BUY"))
                .andExpect(jsonPath("$.leverageRatio").value("SPOT"));

        verify(leverageAccessValidator).validate("user1", LeverageRatio.SPOT);
        verify(orderIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문에 username을 넣어도 무시하고 인증된 사용자명으로 주문한다")
    void order_ignoresUsernameInBody() throws Exception {
        given(orderIngressService.receive("user1", REQUEST)).willReturn(
                new OrderResponse("user1", "005930", OrderType.BUY, 70_000, 10, LeverageRatio.SPOT));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "orderType": "BUY", "orderPrice": 70000, "orderQuantity": 10,
                         "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(orderIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("레버리지 배율을 지정하면 그 배율로 등급을 검증한다")
    void order_withLeverage() throws Exception {
        OrderRequest leverageRequest = new OrderRequest("005930", OrderType.BUY, 70_000, 10, LeverageRatio.X2, null);
        given(orderIngressService.receive("user1", leverageRequest)).willReturn(
                new OrderResponse("user1", "005930", OrderType.BUY, 70_000, 10, LeverageRatio.X2));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "orderType": "BUY", "orderPrice": 70000, "orderQuantity": 10,
                         "leverageRatio": "X2"}
                        """))
                .andExpect(status().isOk());

        verify(leverageAccessValidator).validate("user1", LeverageRatio.X2);
    }

    @Test
    @DisplayName("레버리지 등급이 부족하면 400을 반환하고 주문을 접수하지 않는다")
    void order_leverageNotAllowed() throws Exception {
        willThrow(new LeverageNotAllowedException("GOLD"))
                .given(leverageAccessValidator).validate("user1", LeverageRatio.X2);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "orderType": "BUY", "orderPrice": 70000, "orderQuantity": 10,
                         "leverageRatio": "X2"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 레버리지를 사용하려면 GOLD 등급 이상이어야 합니다."));

        verifyNoInteractions(orderIngressService);
    }

    @Test
    @DisplayName("장이 닫힌 종목이면 등급 검증 전에 거부하고 주문을 접수하지 않는다")
    void order_marketClosed() throws Exception {
        given(bffServerMarketPhaseRegistry.isClosed("005930")).willReturn(true);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(new MarketClosedException().getStatus().value()));

        verifyNoInteractions(leverageAccessValidator, orderIngressService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"orderType\": \"BUY\", \"orderPrice\": 70000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"\", \"orderType\": \"BUY\", \"orderPrice\": 70000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"orderPrice\": 70000, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"orderType\": \"BUY\", \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"orderType\": \"BUY\", \"orderPrice\": 0, \"orderQuantity\": 10}",
            "{\"stockCode\": \"005930\", \"orderType\": \"BUY\", \"orderPrice\": 70000, \"orderQuantity\": -1}"
    })
    @DisplayName("필수값이 없거나 가격·수량이 0 이하면 400을 반환하고 주문을 접수하지 않는다")
    void order_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(leverageAccessValidator, orderIngressService);
    }

    @Test
    @DisplayName("알 수 없는 주문 유형이면 400을 반환한다")
    void order_unknownOrderType() throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "orderType": "HOLD", "orderPrice": 70000, "orderQuantity": 10}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void order_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void order_nullPrincipal() {
        OrderController controller =
                new OrderController(orderIngressService, bffServerMarketPhaseRegistry, leverageAccessValidator);

        assertThat(controller.order(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(orderIngressService);
    }
}
