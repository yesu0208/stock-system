package arile.toy.stocksystem.bffserver.admin.controller;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.service.AccountCalculator;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import arile.toy.stocksystem.bffserver.exception.admin.AdminUserAccountNotFoundException;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import arile.toy.stocksystem.bffserver.portfolio.service.PortfolioCalculator;
import arile.toy.stocksystem.bffserver.rank.client.RankApiClient;
import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
import arile.toy.stocksystem.bffserver.user.dto.Role;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class AdminControllerTest {

    private static final String BASE = "/api/v1/admin/users";
    private static final String TARGET = "user1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private RankApiClient rankApiClient;
    @MockitoBean private AccountCalculator accountCalculator;
    @MockitoBean private PortfolioCalculator portfolioCalculator;
    @MockitoBean private BffServerOrderResponseRepository orderResponseRepository;
    @MockitoBean private BffServerAutoOrderResponseRepository autoOrderResponseRepository;
    @MockitoBean private BffServerOtocoResponseRepository otocoResponseRepository;
    @MockitoBean private BffServerTrailingStopResponseRepository trailingStopResponseRepository;
    @MockitoBean private BffServerAlertResponseRepository alertResponseRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private SlackNotifier slackNotifier;

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    private static UserDto dto(String username) {
        return new UserDto(1L, username, "닉네임", Instant.parse("2026-09-01T00:00:00Z"), Role.USER, null, null);
    }

    private static RankResponse rank(String username, String tier) {
        return new RankResponse(username, tier, 3, 2000L, tier, 1900L, 2200L);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"", "/user1", "/user1/account", "/user1/portfolio", "/user1/orders",
            "/user1/auto-orders", "/user1/otocos", "/user1/trailing-stops", "/user1/alerts"})
    @DisplayName("일반 사용자가 관리자 API를 호출하면 403을 반환하고 아무것도 조회하지 않는다")
    void nonAdmin_forbidden(String path) throws Exception {
        mockMvc.perform(get(BASE + path).with(user("user1").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, accountCalculator, portfolioCalculator, orderResponseRepository);
    }

    @Test
    @DisplayName("GET /admin/users: 전체 사용자 목록에 각자의 랭크를 붙여 반환한다 (랭크 조회 실패 시 랭크 없음)")
    void getAllUsers() throws Exception {
        given(userService.getAllUsers()).willReturn(List.of(dto("user1"), dto("user2")));
        given(rankApiClient.getRank("user1")).willReturn(rank("user1", "GOLD"));
        given(rankApiClient.getRank("user2")).willReturn(null);

        mockMvc.perform(get(BASE).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank.tier").value("GOLD"))
                .andExpect(jsonPath("$[1].rank").doesNotExist());
    }

    @Test
    @DisplayName("GET /admin/users/{username}: 사용자 정보에 랭크를 붙여 반환한다")
    void getUser() throws Exception {
        given(userService.getUserByUsername(TARGET)).willReturn(dto(TARGET));
        given(rankApiClient.getRank(TARGET)).willReturn(rank(TARGET, "SILVER"));

        mockMvc.perform(get(BASE + "/" + TARGET).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TARGET))
                .andExpect(jsonPath("$.rank.tier").value("SILVER"));
    }

    @Test
    @DisplayName("GET /admin/users/{username}/account: 대상 사용자의 계좌 계산 결과를 반환한다")
    void getAccount() throws Exception {
        AccountResponse account = mock(AccountResponse.class);
        given(account.username()).willReturn(TARGET);
        given(accountCalculator.calculate(TARGET)).willReturn(account);

        mockMvc.perform(get(BASE + "/" + TARGET + "/account").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TARGET));

        verify(accountCalculator).calculate(TARGET);
    }

    @Test
    @DisplayName("GET /admin/users/{username}/portfolio: 대상 사용자의 포트폴리오 계산 결과를 반환한다")
    void getPortfolio() throws Exception {
        given(portfolioCalculator.calculate(TARGET))
                .willReturn(new PortfolioResponse(TARGET, 1_000_000L, 500_000L, 50.0, List.of()));

        mockMvc.perform(get(BASE + "/" + TARGET + "/portfolio").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TARGET))
                .andExpect(jsonPath("$.cashRatio").value(50.0));
    }

    @Test
    @DisplayName("계좌·포트폴리오: 계좌 데이터가 없으면 관리자용 '계좌 없음' 예외의 상태 코드로 응답하고 Slack 알림은 보내지 않는다")
    void accountNotFound() throws Exception {
        given(accountCalculator.calculate(TARGET)).willThrow(new RedisAccountNotFoundException("no account"));
        given(portfolioCalculator.calculate(TARGET)).willThrow(new RedisAccountNotFoundException("no account"));
        int expectedStatus = new AdminUserAccountNotFoundException(TARGET).getStatus().value();

        mockMvc.perform(get(BASE + "/" + TARGET + "/account").with(admin()))
                .andExpect(status().is(expectedStatus));
        mockMvc.perform(get(BASE + "/" + TARGET + "/portfolio").with(admin()))
                .andExpect(status().is(expectedStatus));

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("미체결 주문·자동 주문·OTOCO·트레일링 스탑·알림 목록을 대상 사용자 기준으로 조회한다")
    void pendingLists() throws Exception {
        given(orderResponseRepository.findAll(TARGET)).willReturn(List.of());
        given(autoOrderResponseRepository.findAll(TARGET)).willReturn(List.of());
        given(otocoResponseRepository.findAll(TARGET)).willReturn(List.of());
        given(trailingStopResponseRepository.findAll(TARGET)).willReturn(List.of());
        given(alertResponseRepository.findAll(TARGET)).willReturn(List.of());

        for (String resource : List.of("orders", "auto-orders", "otocos", "trailing-stops", "alerts")) {
            mockMvc.perform(get(BASE + "/" + TARGET + "/" + resource).with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        verify(orderResponseRepository).findAll(TARGET);
        verify(autoOrderResponseRepository).findAll(TARGET);
        verify(otocoResponseRepository).findAll(TARGET);
        verify(trailingStopResponseRepository).findAll(TARGET);
        verify(alertResponseRepository).findAll(TARGET);
    }
}
