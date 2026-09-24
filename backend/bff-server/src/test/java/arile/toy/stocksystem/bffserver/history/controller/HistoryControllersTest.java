package arile.toy.stocksystem.bffserver.history.controller;

import arile.toy.stocksystem.bffserver.admin.service.AdminAccessService;
import arile.toy.stocksystem.bffserver.autoorder.client.AutoOrderHistoryApiClient;
import arile.toy.stocksystem.bffserver.autoorder.controller.AutoOrderHistoryController;
import arile.toy.stocksystem.bffserver.exception.admin.AdminAccessDeniedException;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.order.client.OrderHistoryApiClient;
import arile.toy.stocksystem.bffserver.order.controller.OrderHistoryController;
import arile.toy.stocksystem.bffserver.otoco.client.OtocoHistoryApiClient;
import arile.toy.stocksystem.bffserver.otoco.controller.OtocoHistoryController;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.trailingstop.client.TrailingStopHistoryApiClient;
import arile.toy.stocksystem.bffserver.trailingstop.controller.TrailingStopHistoryController;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrderHistoryController.class, AutoOrderHistoryController.class,
        TrailingStopHistoryController.class, OtocoHistoryController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, AdminAccessService.class})
@ActiveProfiles("test")
class HistoryControllersTest {

    private static final HistoryPageResponse<?> PAGE = new HistoryPageResponse<>(List.of(), 1, 20, 25L, true);
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-24T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private OrderHistoryApiClient orderClient;
    @MockitoBean private AutoOrderHistoryApiClient autoOrderClient;
    @MockitoBean private TrailingStopHistoryApiClient trailingStopClient;
    @MockitoBean private OtocoHistoryApiClient otocoClient;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Autowired private OrderHistoryController orderController;
    @Autowired private AutoOrderHistoryController autoOrderController;
    @Autowired private TrailingStopHistoryController trailingStopController;
    @Autowired private OtocoHistoryController otocoController;

    /** 클라이언트 목의 이력 조회 메서드 호출 (목 설정과 검증에 같은 호출을 재사용) */
    @FunctionalInterface
    interface ClientCall {
        Object call(Object client, String username, String stockCode, Instant from, Instant to, int page, int size);
    }

    /** (엔드포인트 경로, 이 엔드포인트가 쓰는 클라이언트 목, 그 목에서 호출하는 메서드) */
    record Endpoint(String path, Function<HistoryControllersTest, Object> client, ClientCall call) {
        @Override
        public String toString() {
            return path;
        }
    }

    static Stream<Endpoint> endpoints() {
        Function<HistoryControllersTest, Object> order = t -> t.orderClient;
        Function<HistoryControllersTest, Object> autoOrder = t -> t.autoOrderClient;
        Function<HistoryControllersTest, Object> trailingStop = t -> t.trailingStopClient;
        Function<HistoryControllersTest, Object> otoco = t -> t.otocoClient;

        return Stream.of(
                new Endpoint("/api/v1/orders/history", order,
                        (c, u, s, f, t, p, z) -> ((OrderHistoryApiClient) c).getHistory(u, s, f, t, p, z)),
                new Endpoint("/api/v1/orders/cancels", order,
                        (c, u, s, f, t, p, z) -> ((OrderHistoryApiClient) c).getCancels(u, s, f, t, p, z)),
                new Endpoint("/api/v1/orders/unfilled", order,
                        (c, u, s, f, t, p, z) -> ((OrderHistoryApiClient) c).getUnfilled(u, s, f, t, p, z)),
                new Endpoint("/api/v1/orders/trades", order,
                        (c, u, s, f, t, p, z) -> ((OrderHistoryApiClient) c).getTrades(u, s, f, t, p, z)),

                new Endpoint("/api/v1/auto-orders/history", autoOrder,
                        (c, u, s, f, t, p, z) -> ((AutoOrderHistoryApiClient) c).getHistory(u, s, f, t, p, z)),
                new Endpoint("/api/v1/auto-orders/cancels", autoOrder,
                        (c, u, s, f, t, p, z) -> ((AutoOrderHistoryApiClient) c).getCancels(u, s, f, t, p, z)),
                new Endpoint("/api/v1/auto-orders/unfilled", autoOrder,
                        (c, u, s, f, t, p, z) -> ((AutoOrderHistoryApiClient) c).getUnfilled(u, s, f, t, p, z)),
                new Endpoint("/api/v1/auto-orders/triggered", autoOrder,
                        (c, u, s, f, t, p, z) -> ((AutoOrderHistoryApiClient) c).getTriggered(u, s, f, t, p, z)),

                new Endpoint("/api/v1/trailing-stops/history", trailingStop,
                        (c, u, s, f, t, p, z) -> ((TrailingStopHistoryApiClient) c).getHistory(u, s, f, t, p, z)),
                new Endpoint("/api/v1/trailing-stops/cancels", trailingStop,
                        (c, u, s, f, t, p, z) -> ((TrailingStopHistoryApiClient) c).getCancels(u, s, f, t, p, z)),
                new Endpoint("/api/v1/trailing-stops/unfilled", trailingStop,
                        (c, u, s, f, t, p, z) -> ((TrailingStopHistoryApiClient) c).getUnfilled(u, s, f, t, p, z)),
                new Endpoint("/api/v1/trailing-stops/triggered", trailingStop,
                        (c, u, s, f, t, p, z) -> ((TrailingStopHistoryApiClient) c).getTriggered(u, s, f, t, p, z)),

                new Endpoint("/api/v1/otocos/history", otoco,
                        (c, u, s, f, t, p, z) -> ((OtocoHistoryApiClient) c).getHistory(u, s, f, t, p, z)),
                new Endpoint("/api/v1/otocos/cancels", otoco,
                        (c, u, s, f, t, p, z) -> ((OtocoHistoryApiClient) c).getCancels(u, s, f, t, p, z)),
                new Endpoint("/api/v1/otocos/unfilled", otoco,
                        (c, u, s, f, t, p, z) -> ((OtocoHistoryApiClient) c).getUnfilled(u, s, f, t, p, z)),
                new Endpoint("/api/v1/otocos/completed", otoco,
                        (c, u, s, f, t, p, z) -> ((OtocoHistoryApiClient) c).getCompleted(u, s, f, t, p, z))
        );
    }

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    /**
     * 이 엔드포인트의 클라이언트가 어떤 인자로 호출되든 response를 돌려주도록 설정한다.
     * 클라이언트마다 반환 제네릭 타입이 달라 given(...).willReturn(...) 대신 doReturn을 사용
     */
    private void givenClientReturns(Endpoint endpoint, Object response) {
        Object stubbing = doReturn(response).when(endpoint.client().apply(this));
        endpoint.call().call(stubbing, anyString(), any(), any(), any(), anyInt(), anyInt());
    }

    /** 이 엔드포인트의 클라이언트가 정확히 이 인자로 호출되었는지 확인한다 */
    private void verifyClientCalled(Endpoint endpoint, String username, String stockCode, Instant from, Instant to,
                                    int page, int size) {
        Object verifying = verify(endpoint.client().apply(this));
        endpoint.call().call(verifying, username, stockCode, from, to, page, size);
    }

    private void verifyNoClientCalled() {
        verifyNoInteractions(orderClient, autoOrderClient, trailingStopClient, otocoClient);
    }

    // ===================== 조회 대상 결정 =====================

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("본인 이력: username이 없으면 로그인 사용자 기준으로 조회하고, 필터·페이지를 그대로 넘긴다")
    void ownHistory(Endpoint endpoint) throws Exception {
        givenClientReturns(endpoint, PAGE);

        mockMvc.perform(get(endpoint.path()).with(user("user1"))
                        .param("stockCode", "005930")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-24T00:00:00Z")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.hasNext").value(true));

        verifyClientCalled(endpoint, "user1", "005930", FROM, TO, 1, 20);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("페이지·개수를 생략하면 0페이지·20개로 조회한다")
    void defaultPaging(Endpoint endpoint) throws Exception {
        givenClientReturns(endpoint, PAGE);

        mockMvc.perform(get(endpoint.path()).with(user("user1")))
                .andExpect(status().isOk());

        verifyClientCalled(endpoint, "user1", null, null, null, 0, 20);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("일반 사용자가 다른 사람의 이력을 요청하면 거부하고 내부 서버를 호출하지 않는다")
    void otherUsersHistory_denied(Endpoint endpoint) throws Exception {
        mockMvc.perform(get(endpoint.path()).param("username", "victim").with(user("user1")))
                .andExpect(status().is(new AdminAccessDeniedException().getStatus().value()));

        verifyNoClientCalled();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("관리자는 username으로 다른 사용자의 이력을 조회한다")
    void adminViewsOtherUser(Endpoint endpoint) throws Exception {
        givenClientReturns(endpoint, PAGE);

        mockMvc.perform(get(endpoint.path()).param("username", "user1").with(admin()))
                .andExpect(status().isOk());

        verifyClientCalled(endpoint, "user1", null, null, null, 0, 20);
    }

    // ===================== 오류 =====================

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("내부 서버 호출이 실패하면(null) 500을 반환한다")
    void clientFails_500(Endpoint endpoint) throws Exception {
        givenClientReturns(endpoint, null);

        mockMvc.perform(get(endpoint.path()).with(user("user1")))
                .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("기간 형식이 ISO 시각이 아니면 400을 반환하고 내부 서버를 호출하지 않는다")
    void invalidDate_400(Endpoint endpoint) throws Exception {
        mockMvc.perform(get(endpoint.path()).param("from", "2026/09/01").with(user("user1")))
                .andExpect(status().isBadRequest());

        verifyNoClientCalled();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("로그인하지 않으면 401을 반환한다")
    void unauthenticated_401(Endpoint endpoint) throws Exception {
        mockMvc.perform(get(endpoint.path()))
                .andExpect(status().isUnauthorized());

        verifyNoClientCalled();
    }

    // ===================== 방어 코드 =====================

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 16개 엔드포인트 모두 401을 반환한다")
    void nullPrincipal_401() {
        List<Supplier<ResponseEntity<?>>> calls = List.of(
                () -> orderController.getHistory(null, null, null, null, null, 0, 20),
                () -> orderController.getCancels(null, null, null, null, null, 0, 20),
                () -> orderController.getUnfilled(null, null, null, null, null, 0, 20),
                () -> orderController.getTrades(null, null, null, null, null, 0, 20),
                () -> autoOrderController.getHistory(null, null, null, null, null, 0, 20),
                () -> autoOrderController.getCancels(null, null, null, null, null, 0, 20),
                () -> autoOrderController.getUnfilled(null, null, null, null, null, 0, 20),
                () -> autoOrderController.getTriggered(null, null, null, null, null, 0, 20),
                () -> trailingStopController.getHistory(null, null, null, null, null, 0, 20),
                () -> trailingStopController.getCancels(null, null, null, null, null, 0, 20),
                () -> trailingStopController.getUnfilled(null, null, null, null, null, 0, 20),
                () -> trailingStopController.getTriggered(null, null, null, null, null, 0, 20),
                () -> otocoController.getHistory(null, null, null, null, null, 0, 20),
                () -> otocoController.getCancels(null, null, null, null, null, 0, 20),
                () -> otocoController.getUnfilled(null, null, null, null, null, 0, 20),
                () -> otocoController.getCompleted(null, null, null, null, null, 0, 20));

        assertThat(calls).allSatisfy(call ->
                assertThat(call.get().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoClientCalled();
    }
}
