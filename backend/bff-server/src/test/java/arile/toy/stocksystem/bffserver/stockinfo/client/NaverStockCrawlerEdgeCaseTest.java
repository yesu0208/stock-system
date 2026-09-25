package arile.toy.stocksystem.bffserver.stockinfo.client;

import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 네이버 API 실패, 빈 응답, 일부 필드가 빠진 응답 처리 */
class NaverStockCrawlerEdgeCaseTest {

    private static final String BASE = "https://stock.naver.com";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockRestServiceServer server;
    private NaverStockCrawlerClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new NaverStockCrawlerClient(builder.build());
    }

    private void respond(String pathPrefix, String json) {
        server.expect(requestTo(startsWith(BASE + pathPrefix)))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private void fail(String pathPrefix) {
        server.expect(requestTo(startsWith(BASE + pathPrefix))).andRespond(withServerError());
    }

    private static String json(Object result) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(result);
    }

    // ===================== 종목 =====================

    @Test
    @DisplayName("부가정보: 소개 문구가 null이면 건너뛰고, 증권사 목록이 없으면 빈 목록, 시장 구분 응답이 비면 빈 값")
    void detailExtra_partialResponse() throws Exception {
        respond("/api/domestic/detail/005930/detail",
                "{\"itemcode\": \"005930\", \"comment1\": \"반도체\", \"manageStatusGb\": \"0\"}");
        respond("/api/domestic/detail/005930/sosok", "null");
        respond("/api/domestic/detail/005930/traderInfo", "{\"sellQuant\": \"1\"}");

        String result = json(client.getStockDetailExtra("005930"));

        assertThat(result).contains("\"반도체\"", "\"외국계 합계\"").doesNotContain("관리종목");
    }

    // ===================== 외국인·기관 매매 =====================

    @Test
    @DisplayName("외국인·기관 매매: 다음 페이지에 항목이 있으면 hasNext=true, 응답이 null이면 빈 목록")
    void foreignTrades_hasNextAndNull() throws Exception {
        server.expect(requestTo(containsString("startIdx=0")))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("startIdx=1")))
                .andRespond(withSuccess("[{\"bizdate\": \"20260923\"}]", MediaType.APPLICATION_JSON));

        String result = json(client.getForeignInstitutionTrades("005930", 1));

        assertThat(result).contains("true");
    }

    @Test
    @DisplayName("외국인·기관 매매: API가 실패하면 IllegalStateException")
    void foreignTrades_fails() {
        fail("/api/domestic/detail/005930/trend");

        assertThatThrownBy(() -> client.getForeignInstitutionTrades("005930", 1))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===================== 지수 =====================

    @Test
    @DisplayName("지수: 하락·기타 등락, 상세 정보가 비어 있는 응답도 빈 값·null로 채운다")
    void marketIndices_partialResponses() throws Exception {
        respond("/api/securityFe/api/index/KOSPI/basic", "{\"closePrice\": \"2,600\"}");
        respond("/api/securityFe/api/index/KOSPI/integration", "{}");
        respond("/api/securityFe/api/index/KOSDAQ/basic", "{\"compareToPreviousPrice\": {\"name\": \"FALLING\"}}");
        respond("/api/securityFe/api/index/KOSDAQ/integration", "{}");
        respond("/api/securityFe/api/index/KPI200/basic", "{\"compareToPreviousPrice\": {\"name\": \"EVEN\"}}");
        respond("/api/securityFe/api/index/KPI200/integration", "{}");

        String result = json(client.getMarketIndices());

        assertThat(result).contains("\"2,600\"", "\"DOWN\"", "\"STEADY\"");
    }

    @Test
    @DisplayName("지수: 응답이 비어 있으면 IllegalStateException")
    void marketIndices_emptyResponse() {
        respond("/api/securityFe/api/index/KOSPI/basic", "null");
        respond("/api/securityFe/api/index/KOSPI/integration", "{}");

        assertThatThrownBy(() -> client.getMarketIndices()).isInstanceOf(IllegalStateException.class);
    }

    // ===================== 업종·환율 =====================

    @Test
    @DisplayName("업종 목록: 모든 등락률이 0이면 그래프 비율은 0")
    void allUpjongs_allZero() throws Exception {
        respond("/api/stockSecurity/rankings/v2/domestic/industries",
                "{\"hasNext\": false, \"items\": [{\"name\": \"보합업종\", \"changeRate\": \"0\"}]}");

        assertThat(json(client.getAllUpjongs())).contains("\"보합업종\"", "\"0\"");
    }

    @Test
    @DisplayName("업종 목록: 응답·항목이 비어 있거나 API가 실패하면 IllegalStateException")
    void allUpjongs_failures() {
        respond("/api/stockSecurity/rankings/v2/domestic/industries", "null");
        respond("/api/stockSecurity/rankings/v2/domestic/industries", "{\"hasNext\": false}");
        fail("/api/stockSecurity/rankings/v2/domestic/industries");

        assertThatThrownBy(() -> client.getAllUpjongs()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> client.getAllUpjongs()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> client.getAllUpjongs()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("업종별 종목: API가 실패하면 IllegalStateException")
    void upjongStocks_fails() {
        fail("/api/domestic/market/upjong/278/stocklist");

        assertThatThrownBy(() -> client.getUpjongStocks("278")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("환율: 응답이 null이면 빈 목록, API가 실패하면 IllegalStateException")
    void exchangeRates_emptyOrFails() throws Exception {
        respond("/api/securityService/marketindex/majors/rpc", "null");
        fail("/api/securityService/marketindex/majors/rpc");

        assertThat(client.getExchangeRates()).isEmpty();
        assertThatThrownBy(() -> client.getExchangeRates()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("환율: 기준 시각이 있으면 그대로 담는다")
    void exchangeRates_withTime() throws Exception {
        respond("/api/securityService/marketindex/majors/rpc",
                "[{\"categoryType\": \"exchange\", \"name\": \"USD\", \"localTradedAt\": \"2026-09-24T09:00\"}]");

        assertThat(json(client.getExchangeRates())).contains("\"2026-09-24T09:00\"");
    }

    // ===================== 투자자 동향 =====================

    @Test
    @DisplayName("투자자 동향: 응답이 null이면 빈 목록, 마지막이 아니면 hasNext=true, 시간별은 시각을 쓰고 금액 목록이 없으면 0")
    void investorTrend_variants() throws Exception {
        respond("/api/domestic/market/trend/", "null");
        respond("/api/domestic/market/trend/",
                "{\"last\": false, \"content\": [{\"bizdate\": \"20260924\", \"time\": \"0930\"}]}");

        assertThat(json(client.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1))).contains("[]");
        assertThat(json(client.getInvestorTrend(MarketType.KOSPI, TrendType.TIME, 1)))
                .contains("\"0930\"", "true")
                .doesNotContain("20260924");
    }

    // ===================== 수급 순위 =====================

    @Test
    @DisplayName("수급 순위: 응답이 null이면 빈 결과, 매도 순위가 없으면 빈 날짜, 숫자가 아닌 거래량은 null")
    void dealRank_variants() throws Exception {
        respond("/api/domestic/market/trend/trendForeignOrg", "null");
        respond("/api/domestic/market/trend/trendForeignOrg", "{\"sections\": {}}");
        respond("/api/domestic/market/trend/trendForeignOrg", """
                {"sections": {"sellRankList": [
                  {"itemcode": "005930", "bizdateFrom": "20260924", "bizdateTo": "20260924", "accTradeVolume": "abc"}]}}
                """);

        DealRankMarket market = DealRankMarket.values()[0];
        InvestorType investor = InvestorType.values()[0];
        PeriodType period = PeriodType.values()[0];

        assertThat(json(client.getDealRank(market, investor, DealType.BUY, period))).contains("[]");
        assertThat(json(client.getDealRank(market, investor, DealType.SELL, period))).contains("\"\"");
        assertThat(json(client.getDealRank(market, investor, DealType.SELL, period)))
                .contains("\"2026.09.24\"")
                .doesNotContain("2026.09.24 ~");
    }

    @Test
    @DisplayName("수급 순위: API가 실패하면 IllegalStateException")
    void dealRank_fails() {
        fail("/api/domestic/market/trend/trendForeignOrg");

        assertThatThrownBy(() -> client.getDealRank(DealRankMarket.values()[0], InvestorType.values()[0],
                DealType.BUY, PeriodType.values()[0])).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("지수: 기본 정보는 있는데 상세 정보 응답이 비어 있으면 IllegalStateException")
    void marketIndices_integrationEmpty() {
        respond("/api/securityFe/api/index/KOSPI/basic", "{\"closePrice\": \"2,600\"}");
        respond("/api/securityFe/api/index/KOSPI/integration", "null");

        assertThatThrownBy(() -> client.getMarketIndices()).isInstanceOf(IllegalStateException.class);
    }
}
