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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    @DisplayName("로그인 없이 인기 종목·업종 목록·업종별 종목을 조회한다 (공개 API)")
    void publicMarketInfo() throws Exception {
        given(stockInfoService.getPopularStocks()).willReturn(List.of());
        given(stockInfoService.getAllUpjongs()).willReturn(new UpjongResponse(List.of()));
        given(stockInfoService.getUpjongStocks("278")).willReturn(new UpjongStockResponse("반도체", List.of()));

        mockMvc.perform(get(BASE + "/market/popular")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/market/upjong")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/upjong/278"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upjongName").value("반도체"));
    }

    @Test
    @DisplayName("종목 정보·상세 부가정보를 종목코드로 조회한다")
    void stockInfo() throws Exception {
        mockMvc.perform(get(BASE + "/005930")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/005930/detail-extra")).andExpect(status().isOk());

        verify(stockInfoService).getStockInfo("005930");
        verify(stockInfoService).getStockDetailExtra("005930");
    }

    @Test
    @DisplayName("외국인·기관 매매: 페이지를 생략하면 1페이지를 조회한다")
    void foreignTrades_defaultPage() throws Exception {
        given(stockInfoService.getForeignInstitutionTrades("005930", 1)).willReturn(new TradePageResponse(List.of(), false));

        mockMvc.perform(get(BASE + "/005930/foreign")).andExpect(status().isOk());

        verify(stockInfoService).getForeignInstitutionTrades("005930", 1);
    }

    @Test
    @DisplayName("투자자 동향: 시간별·일별을 구분하고, 시장을 생략하면 KOSPI 1페이지를 조회한다")
    void investorTrend() throws Exception {
        given(stockInfoService.getInvestorTrend(MarketType.KOSPI, TrendType.TIME, 1)).willReturn(new TrendResponse(List.of(), false));
        given(stockInfoService.getInvestorTrend(MarketType.KOSDAQ, TrendType.DAY, 2)).willReturn(new TrendResponse(List.of(), true));

        mockMvc.perform(get(BASE + "/investor-trend/time")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/investor-trend/day").param("market", "KOSDAQ").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("수급 순위: 필수 파라미터가 없거나 알 수 없는 값이면 400이고 조회하지 않는다")
    void dealRank_invalid_400() throws Exception {
        mockMvc.perform(get(BASE + "/deal-rank")).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/deal-rank")
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

        mockMvc.perform(get(BASE + "/005930/foreign").param("page", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/market/popular")).andExpect(status().isServiceUnavailable());

        verifyNoInteractions(slackNotifier);
    }
}
