package arile.toy.stocksystem.bffserver.otoco.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.exception.leverage.LeverageNotAllowedException;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.leverage.service.LeverageAccessValidator;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoExitMode;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoRequest;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponse;
import arile.toy.stocksystem.bffserver.otoco.service.OtocoIngressService;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OtocoController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class OtocoControllerTest {

    private static final String URL = "/api/v1/otocos";
    private static final String BODY = """
            {"stockCode": "005930", "entryDirection": "ABOVE", "orderQuantity": 10, "entryTriggerPrice": 70000,
             "tpMode": "PRICE", "tpPrice": 75000, "slMode": "PCT", "slPct": 3.0}
            """;
    private static final OtocoRequest REQUEST = new OtocoRequest("005930", OtocoEntryDirection.ABOVE, 10, 70_000,
            OtocoExitMode.PRICE, 75_000, null, OtocoExitMode.PCT, null, 3.0, null);
    private static final OtocoResponse RESPONSE = new OtocoResponse("user1", "005930", OtocoEntryDirection.ABOVE,
            10, 70_000, OtocoExitMode.PRICE, 75_000, null, OtocoExitMode.PCT, null, 3.0, LeverageRatio.SPOT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private OtocoIngressService otocoIngressService;
    @MockitoBean private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    @MockitoBean private LeverageAccessValidator leverageAccessValidator;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 사용자명으로 레버리지 자격을 검증한 뒤 OTOCO를 접수한다")
    void otoco() throws Exception {
        given(otocoIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.entryTriggerPrice").value(70_000))
                .andExpect(jsonPath("$.tpPrice").value(75_000))
                .andExpect(jsonPath("$.slPct").value(3.0));

        verify(leverageAccessValidator).validate("user1", LeverageRatio.SPOT);
        verify(otocoIngressService).receive("user1", REQUEST);
    }

    @Test
    @DisplayName("요청 본문의 username은 무시하고 인증된 사용자명으로 접수한다")
    void otoco_ignoresUsernameInBody() throws Exception {
        given(otocoIngressService.receive("user1", REQUEST)).willReturn(RESPONSE);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "entryDirection": "ABOVE", "orderQuantity": 10, "entryTriggerPrice": 70000,
                         "tpMode": "PRICE", "tpPrice": 75000, "slMode": "PCT", "slPct": 3.0, "username": "victim"}
                        """))
                .andExpect(status().isOk());

        verify(otocoIngressService).receive("user1", REQUEST);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "익절가가 진입가 이하 | tpValid | {\"stockCode\": \"005930\", \"entryDirection\": \"ABOVE\", \"orderQuantity\": 10, \"entryTriggerPrice\": 70000, \"tpMode\": \"PRICE\", \"tpPrice\": 70000, \"slMode\": \"PCT\", \"slPct\": 3.0}",
            "손절가 0원          | slValid | {\"stockCode\": \"005930\", \"entryDirection\": \"ABOVE\", \"orderQuantity\": 10, \"entryTriggerPrice\": 70000, \"tpMode\": \"PRICE\", \"tpPrice\": 75000, \"slMode\": \"PRICE\", \"slPrice\": 0}",
            "익절 퍼센트 50%     | tpValid | {\"stockCode\": \"005930\", \"entryDirection\": \"ABOVE\", \"orderQuantity\": 10, \"entryTriggerPrice\": 70000, \"tpMode\": \"PCT\", \"tpPct\": 50, \"slMode\": \"PCT\", \"slPct\": 3.0}",
            "손절 모드 값 누락   | slValid | {\"stockCode\": \"005930\", \"entryDirection\": \"ABOVE\", \"orderQuantity\": 10, \"entryTriggerPrice\": 70000, \"tpMode\": \"PRICE\", \"tpPrice\": 75000, \"slMode\": \"PCT\"}"
    })
    @DisplayName("익절·손절 조건이 잘못되면 위반 항목을 담아 400을 반환하고 접수하지 않는다")
    void otoco_invalidExit(String description, String violatedField, String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(violatedField)));

        verifyNoInteractions(leverageAccessValidator, otocoIngressService);
    }

    @Test
    @DisplayName("레버리지 등급이 부족하면 400을 반환하고 접수하지 않는다")
    void otoco_leverageNotAllowed() throws Exception {
        willThrow(new LeverageNotAllowedException("GOLD"))
                .given(leverageAccessValidator).validate("user1", LeverageRatio.X2);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"stockCode": "005930", "entryDirection": "ABOVE", "orderQuantity": 10, "entryTriggerPrice": 70000,
                         "tpMode": "PRICE", "tpPrice": 75000, "slMode": "PCT", "slPct": 3.0, "leverageRatio": "X2"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(otocoIngressService);
    }

    @Test
    @DisplayName("장이 닫힌 종목이면 등급 검증 전에 거부한다")
    void otoco_marketClosed() throws Exception {
        given(bffServerMarketPhaseRegistry.isClosed("005930")).willReturn(true);

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(new MarketClosedException().getStatus().value()));

        verifyNoInteractions(leverageAccessValidator, otocoIngressService);
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    void otoco_unauthenticated() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(otocoIngressService);
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 401을 반환한다")
    void otoco_nullPrincipal() {
        OtocoController controller =
                new OtocoController(otocoIngressService, bffServerMarketPhaseRegistry, leverageAccessValidator);

        assertThat(controller.otoco(REQUEST, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
