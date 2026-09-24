package arile.toy.stocksystem.bffserver.stockinfo.controller;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockInfoService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockInfoController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class StockInfoControllerTest {

    private static final String BASE = "/api/v1/stocks";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private StockInfoService stockInfoService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    private static RequestPostProcessor loggedIn() {
        return user("user1");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/api/v1/stocks/market/popular", "/api/v1/stocks/market/upjong", "/api/v1/stocks/upjong/278",
            "/api/v1/stocks/005930", "/api/v1/stocks/005930/foreign", "/api/v1/stocks/005930/detail-extra",
            "/api/v1/stocks/investor-trend/time", "/api/v1/stocks/investor-trend/day"
    })
    @DisplayName("로그인하지 않으면 401이고 네이버 조회(서비스 호출)가 일어나지 않는다")
    void unauthenticated_401(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());

        verifyNoInteractions(stockInfoService);
    }

    @Test
    @DisplayName("인기 종목·업종 목록·업종별 종목을 조회한다")
    void marketInfo() throws Exception {
        given(stockInfoService.getPopularStocks()).willReturn(List.of());
        given(stockInfoService.getAllUpjongs()).willReturn(new UpjongResponse(List.of()));
        given(stockInfoService.getUpjongStocks("278")).willReturn(new UpjongStockResponse("반도체", List.of()));

        mockMvc.perform(get(BASE + "/market/popular").with(loggedIn())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/market/upjong").with(loggedIn())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/upjong/278").with(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upjongName").value("반도체"));
    }

    @Test
    @DisplayName("종목 정보·상세 부가정보를 종목코드로 조회한다")
    void stockInfo() throws Exception {
        mockMvc.perform(get(BASE + "/005930").with(loggedIn())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/005930/detail-extra").with(loggedIn())).andExpect(status().isOk());

        verify(stockInfoService).getStockInfo("005930");
        verify(stockInfoService).getStockDetailExtra("005930");
    }

    @Test
    @DisplayName("외국인·기관 매매: 페이지를 생략하면 1페이지를 조회한다")
    void foreignTrades_defaultPage() throws Exception {
        given(stockInfoService.getForeignInstitutionTrades("005930", 1))
                .willReturn(new TradePageResponse(List.of(), false));

        mockMvc.perform(get(BASE + "/005930/foreign").with(loggedIn())).andExpect(status().isOk());

        verify(stockInfoService).getForeignInstitutionTrades("005930", 1);
    }

    @Test
    @DisplayName("투자자 동향: 시간별·일별을 구분하고, 시장을 생략하면 KOSPI 1페이지를 조회한다")
    void investorTrend() throws Exception {
        given(stockInfoService.getInvestorTrend(MarketType.KOSPI, TrendType.TIME, 1))
                .willReturn(new TrendResponse(List.of(), false));
        given(stockInfoService.getInvestorTrend(MarketType.KOSDAQ, TrendType.DAY, 2))
                .willReturn(new TrendResponse(List.of(), true));

        mockMvc.perform(get(BASE + "/investor-trend/time").with(loggedIn())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/investor-trend/day").with(loggedIn())
                        .param("market", "KOSDAQ").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("수급 순위: 필수 파라미터가 없거나 알 수 없는 값이면 400이고 조회하지 않는다")
    void dealRank_invalid_400() throws Exception {
        mockMvc.perform(get(BASE + "/deal-rank").with(loggedIn())).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/deal-rank").with(loggedIn())
                        .param("market", "UNKNOWN").param("investorType", "UNKNOWN").param("dealType", "BUY"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockInfoService, slackNotifier);
    }

    @Test
    @DisplayName("페이지가 1 미만이면 400, 네이버 장애면 503으로 응답하고 Slack 알림은 보내지 않는다")
    void serviceErrors() throws Exception {
        given(stockInfoService.getForeignInstitutionTrades("005930", 0))
                .willThrow(new ClientErrorException(HttpStatus.BAD_REQUEST, "page는 1 이상이어야 합니다."));
        given(stockInfoService.getPopularStocks())
                .willThrow(new ClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "시세 정보를 불러오지 못했습니다."));

        mockMvc.perform(get(BASE + "/005930/foreign").with(loggedIn()).param("page", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/market/popular").with(loggedIn()))
                .andExpect(status().isServiceUnavailable());

        verifyNoInteractions(slackNotifier);
    }
}
