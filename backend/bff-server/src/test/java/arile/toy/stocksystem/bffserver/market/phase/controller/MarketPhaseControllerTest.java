package arile.toy.stocksystem.bffserver.market.phase.controller;

import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhase;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketPhaseController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class MarketPhaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private BffServerMarketPhaseRegistry registry;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @ParameterizedTest(name = "{0}")
    @EnumSource(BffServerMarketPhase.class)
    @DisplayName("로그인 없이 전체 장 상태를 phase 필드로 조회한다 (로그인 화면의 장 상태 표시용)")
    void getCurrentPhase(BffServerMarketPhase phase) throws Exception {
        given(registry.getGlobalPhase()).willReturn(phase);

        mockMvc.perform(get("/api/v1/market/phase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value(phase.name()));
    }
}
