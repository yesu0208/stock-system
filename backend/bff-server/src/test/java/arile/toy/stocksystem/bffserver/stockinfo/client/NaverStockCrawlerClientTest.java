package arile.toy.stocksystem.bffserver.stockinfo.client;

import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverStockCrawlerClientTest {

    private static final String BASE = "https://stock.naver.com";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockRestServiceServer server;
    private NaverStockCrawlerClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json");
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

    /** 결과를 JSON으로 바꿔 화면에 나갈 값이 들어 있는지 확인 (DTO 필드 이름과 무관) */
    private static String json(Object result) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(result);
    }

    private static final String DETAIL_JSON = """
            {"itemcode": "005930", "itemname": "삼성전자", "tradeTime": "20260924153000",
             "manageStatusGb": "1", "marketAlertType": "02",
             "nowPrice": "71000", "openPrice": "70000", "highPrice": "71500", "lowPrice": "69800",
             "upperLimitPrice": "91000", "lowerLimitPrice": "49000",
             "upDownGb": "2", "prevChangePrice": "1000", "prevChangeRate": "1.43",
             "tradeVolume": "1234567", "tradeAmount": "87654321000", "marketSum": "4230000",
             "listedStockCnt": "5969782550", "facePrice": "100", "unit": "1",
             "frgnLimitCnt": "5969782550", "frgnHoldCnt": "3000000000", "frgnAcqRatio": "50.256",
             "per": "12.3", "eps": "5000", "estimatedPer": "10.1", "estimatedEps": "6000",
             "pbr": "1.2", "bps": "50000", "dividendRate": "2.1",
             "week52HighPrice": "88000", "week52LowPrice": "60000",
             "comment1": "반도체 제조", "comment2": "", "comment3": "스마트폰 제조",
             "sameIndustryPer": "15.0", "sameIndustryChangeRate": "-1.5"}
            """;

    @Test
    @DisplayName("기본 생성자는 네트워크 없이 네이버용 클라이언트를 만든다 (운영 경로)")
    void defaultConstructor() {
        assertThatCode(NaverStockCrawlerClient::new).doesNotThrowAnyException();
    }

    // ===================== 종목 =====================

    @Nested
    @DisplayName("종목 요약·정보·부가정보")
    class Stock {

        @Test
        @DisplayName("종목 요약: 가격에 콤마, 등락률에 부호·%, 방향 코드 변환, 전일 종가(현재가-대비)를 계산한다")
        void detailSummary() throws Exception {
            respond("/api/domestic/detail/005930/detail", DETAIL_JSON);
            respond("/api/domestic/detail/005930/sosok", "{\"sosok\": \"KOSPI\"}");

            String result = json(client.getStockDetailSummary("005930"));

            assertThat(result).contains("\"005930\"", "\"삼성전자\"", "\"KOSPI\"", "\"71,000\"", "\"1,000\"",
                    "\"+1.43%\"", "\"UP\"", "\"70,000\"", "\"91,000\"", "\"1,234,567\"");
        }

        @Test
        @DisplayName("종목 요약: 시장 구분 조회가 실패해도 빈 값으로 요약을 만든다")
        void detailSummary_sosokFails() throws Exception {
            respond("/api/domestic/detail/005930/detail", DETAIL_JSON);
            fail("/api/domestic/detail/005930/sosok");

            assertThat(json(client.getStockDetailSummary("005930"))).contains("\"71,000\"");
        }

        @Test
        @DisplayName("종목 상세 API가 실패하면 IllegalStateException (서비스에서 503으로 변환)")
        void detailFails() {
            fail("/api/domestic/detail/005930/detail");

            assertThatThrownBy(() -> client.getStockDetailSummary("005930"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("종목 정보: 수치 포맷과 컨센서스(의견·목표가·날짜)를 담는다")
        void stockInfo() {
            respond("/api/domestic/detail/005930/detail", DETAIL_JSON);
            respond("/api/domestic/detail/005930/consensus",
                    "{\"date\": \"20260920\", \"opinion\": \"매수\", \"targetPrice\": \"90000\"}");

            StockInfo info = client.getStockInfo("005930");

            assertThat(info.marketCap()).isEqualTo("4,230,000");
            assertThat(info.foreignRate()).isEqualTo("50.26%");
            assertThat(info.opinion()).isEqualTo("매수");
            assertThat(info.targetPrice()).isEqualTo("90,000");
            assertThat(info.consensusDate()).isEqualTo("2026.09.20");
            assertThat(info.dividendYield()).isEqualTo("2.10%");
            assertThat(info.sameIndustryRate()).isEqualTo("-1.50%");
        }

        @Test
        @DisplayName("종목 정보: 컨센서스 조회가 실패하면 의견·목표가·날짜만 비운다")
        void stockInfo_consensusFails() {
            respond("/api/domestic/detail/005930/detail", DETAIL_JSON);
            fail("/api/domestic/detail/005930/consensus");

            StockInfo info = client.getStockInfo("005930");

            assertThat(info.opinion()).isEmpty();
            assertThat(info.targetPrice()).isEmpty();
            assertThat(info.marketCap()).isEqualTo("4,230,000");
        }

        @Test
        @DisplayName("부가정보: 회사 소개(빈 줄 제외), 투자경고·관리종목, 매도·매수 상위 증권사, 외국계 합계를 담는다")
        void detailExtra() throws Exception {
            respond("/api/domestic/detail/005930/detail", DETAIL_JSON);
            respond("/api/domestic/detail/005930/sosok", "{\"sosok\": \"KOSPI\"}");
            respond("/api/domestic/detail/005930/traderInfo", """
                    {"sellQuant": "1000", "buyQuant": "3000", "quant": "-2000",
                     "traderList": [
                       {"display_name": "A증권", "sellQuant": "500", "buyQuant": "100"},
                       {"display_name": "B증권", "sellQuant": "100", "buyQuant": "900"}]}
                    """);

            String result = json(client.getStockDetailExtra("005930"));

            assertThat(result).contains("반도체 제조\\n스마트폰 제조", "\"투자경고\"", "\"관리종목\"",
                    "\"A증권\"", "\"B증권\"", "\"외국계 합계\"", "\"-2,000\"", "\"3,000\"");
        }

        @Test
        @DisplayName("부가정보: 증권사 정보 조회가 실패하면 증권사 목록만 비운다")
        void detailExtra_traderFails() throws Exception {
            respond("/api/domestic/detail/005930/detail", DETAIL_JSON);
            respond("/api/domestic/detail/005930/sosok", "{\"sosok\": \"KOSPI\"}");
            fail("/api/domestic/detail/005930/traderInfo");

            assertThat(json(client.getStockDetailExtra("005930")))
                    .contains("\"투자경고\"")
                    .doesNotContain("A증권");
        }
    }

    // ===================== 외국인·기관 매매 =====================

    @Test
    @DisplayName("외국인·기관 매매: 날짜·등락률·부호 있는 순매수를 변환하고, 다음 페이지가 비면 hasNext=false")
    void foreignTrades() throws Exception {
        server.expect(requestTo(containsString("/trend?tradeType=KRX&startIdx=0")))
                .andRespond(withSuccess("""
                        [{"bizdate": "20260924", "closePrice": "71000", "prevChangePrice": "1000", "upDownGb": "상승",
                          "tradeVolume": "1000", "organPureBuyQuant": "-1500", "individualPureBuyQuant": "2000",
                          "foreignerPureBuyQuant": "-500", "frgnStock": "3000000", "frgnHoldRatio": "50.5"}]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/trend?tradeType=KRX&startIdx=1")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = json(client.getForeignInstitutionTrades("005930", 1));

        assertThat(result).contains("\"2026.09.24\"", "\"71,000\"", "\"UP\"", "\"+1.43%\"",
                "\"-1,500\"", "\"2,000\"", "\"50.50%\"", "false");
    }

    // ===================== 시장 =====================

    @Nested
    @DisplayName("시장 정보")
    class Market {

        @Test
        @DisplayName("인기 종목: 1위부터 순위를 매기고 방향 코드를 변환한다")
        void popularStocks() throws Exception {
            respond("/api/domestic/market/stock/default", """
                    [{"itemcode": "005930", "itemname": "삼성전자", "nowPrice": "71000", "upDownGb": "2"},
                     {"itemcode": "000660", "itemname": "SK하이닉스", "nowPrice": "120000", "upDownGb": "5"}]
                    """);

            String result = json(client.getPopularStocks());

            assertThat(result).contains("\"삼성전자\"", "\"UP\"", "\"SK하이닉스\"", "\"DOWN\"");
        }

        @Test
        @DisplayName("인기 종목: 응답이 비었거나 API가 실패하면 IllegalStateException")
        void popularStocks_failures() {
            respond("/api/domestic/market/stock/default", "null");
            fail("/api/domestic/market/stock/default");

            assertThatThrownBy(() -> client.getPopularStocks()).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> client.getPopularStocks()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("지수: 코스피·코스닥·코스피200의 기본값과 상세(전일 종가, 등락 종목 수 등)를 합친다")
        void marketIndices() throws Exception {
            for (String code : new String[]{"KOSPI", "KOSDAQ", "KPI200"}) {
                respond("/api/securityFe/api/index/" + code + "/basic", """
                        {"closePrice": "2,650.12", "compareToPreviousClosePrice": "15.3", "fluctuationsRatio": "0.58",
                         "compareToPreviousPrice": {"name": "RISING"}, "localTradedAt": "2026-09-24T15:30:00+09:00"}
                        """);
                respond("/api/securityFe/api/index/" + code + "/integration", """
                        {"totalInfos": [{"code": "lastClosePrice", "value": "2,634.82"}],
                         "upDownStockInfo": {"upperCount": "3", "riseCount": "500", "steadyCount": "50",
                                             "fallCount": "300", "lowerCount": "1"},
                         "programTrendInfo": {"indexDifferenceReal": "10", "indexBiDifferenceReal": "20", "indexTotalReal": "30"},
                         "dealTrendInfo": {"personalValue": "-100", "foreignValue": "200", "institutionalValue": "-100"}}
                        """);
            }

            String result = json(client.getMarketIndices());

            assertThat(result).contains("\"코스피\"", "\"코스닥\"", "\"코스피200\"", "\"UP\"",
                    "\"2,650.12\"", "\"2,634.82\"", "\"500\"");
        }

        @Test
        @DisplayName("지수: API가 실패하면 IllegalStateException")
        void marketIndices_fails() {
            fail("/api/securityFe/api/index/KOSPI/basic");

            assertThatThrownBy(() -> client.getMarketIndices()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("업종 목록: 등락 종목 수 합계와 가장 큰 등락률 대비 그래프 비율을 계산하고 상위 3개만 담는다")
        void allUpjongs() throws Exception {
            respond("/api/stockSecurity/rankings/v2/domestic/industries", """
                    {"hasNext": false, "items": [
                      {"code": "278", "name": "반도체", "changeRate": "2.0",
                       "risingCount": "10", "fallingCount": "5", "unchangedCount": "1",
                       "topByChangeRate": [{"code": "a", "name": "A"}, {"code": "b", "name": "B"},
                                           {"code": "c", "name": "C"}, {"code": "d", "name": "D4번째"}]},
                      {"code": "300", "name": "은행", "changeRate": "-1.0",
                       "risingCount": "1", "fallingCount": "2", "unchangedCount": "0"}]}
                    """);

            String result = json(client.getAllUpjongs());

            assertThat(result).contains("\"반도체\"", "\"16\"", "\"100\"", "\"은행\"", "\"3\"", "\"50\"")
                    .doesNotContain("D4번째");
        }

        @Test
        @DisplayName("업종별 종목: 가격·등락률·시가총액을 변환하고, 응답이 비면 IllegalStateException")
        void upjongStocks() throws Exception {
            respond("/api/domestic/market/upjong/278/stocklist", """
                    [{"itemcode": "005930", "itemname": "삼성전자", "nowPrice": "71000", "upDownGb": "2",
                      "prevChangePrice": "1000", "prevChangeRate": "1.43", "tradeVolume": "100",
                      "tradeAmount": "7100000", "marketSum": "4230000"}]
                    """);
            respond("/api/domestic/market/upjong/279/stocklist", "null");

            assertThat(json(client.getUpjongStocks("278"))).contains("\"71,000\"", "\"+1.43%\"", "\"4,230,000\"");
            assertThatThrownBy(() -> client.getUpjongStocks("279")).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("환율: exchange 항목만 담고, 등락 정보가 없으면 보합으로 표시한다")
        void exchangeRates() throws Exception {
            respond("/api/securityService/marketindex/majors/rpc", """
                    [{"categoryType": "exchange", "name": "미국 USD", "closePrice": "1,380.5",
                      "fluctuationsType": {"text": "상승"}},
                     {"categoryType": "exchange", "name": "일본 JPY"},
                     {"categoryType": "oil", "name": "WTI"}]
                    """);

            String result = json(client.getExchangeRates());

            assertThat(result).contains("\"미국 USD\"", "\"상승\"", "\"일본 JPY\"", "\"보합\"")
                    .doesNotContain("WTI");
        }
    }

    // ===================== 투자자 동향 · 수급 순위 =====================

    @Nested
    @DisplayName("투자자 동향 · 수급 순위")
    class Trend {

        @Test
        @DisplayName("투자자 동향: 투자자 코드별 금액을 기관·외국인 등으로 합산하고, 마지막 페이지면 hasNext=false")
        void investorTrend() throws Exception {
            respond("/api/domestic/market/trend/", """
                    {"last": true, "content": [
                      {"bizdate": "20260924", "time": "1530", "netAmounts": [
                        {"investorGubun": "1000", "diffValue": "100"}, {"investorGubun": "2000", "diffValue": "200"},
                        {"investorGubun": "8000", "diffValue": "-300"},
                        {"investorGubun": "9000", "diffValue": "40"}, {"investorGubun": "9001", "diffValue": "60"}]}]}
                    """);

            String result = json(client.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1));

            assertThat(result).contains("\"20260924\"", "-300", "100", "300", "false");
        }

        @Test
        @DisplayName("투자자 동향: 응답 내용이 없으면 빈 목록, API가 실패하면 IllegalStateException")
        void investorTrend_emptyOrFails() throws Exception {
            respond("/api/domestic/market/trend/", "{\"last\": true}");
            fail("/api/domestic/market/trend/");

            assertThat(json(client.getInvestorTrend(MarketType.KOSPI, TrendType.TIME, 1))).contains("[]");
            assertThatThrownBy(() -> client.getInvestorTrend(MarketType.KOSPI, TrendType.TIME, 1))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("수급 순위: 매수 순위를 1위부터 담고 기간을 'from ~ to'로 표시한다")
        void dealRank() throws Exception {
            respond("/api/domestic/market/trend/trendForeignOrg", """
                    {"sections": {"buyRankList": [
                      {"itemcode": "005930", "itemname": "삼성전자", "bizdateFrom": "20260920", "bizdateTo": "20260924",
                       "accTradeVolume": "1000", "accTradeAmount": "71000000", "dailyTradeVolume": ""}]}}
                    """);

            String result = json(client.getDealRank(DealRankMarket.values()[0], InvestorType.values()[0],
                    DealType.BUY, PeriodType.values()[0]));

            assertThat(result).contains("\"삼성전자\"", "\"2026.09.20 ~ 2026.09.24\"", "71000000");
        }

        @Test
        @DisplayName("수급 순위: 섹션이 없으면 빈 결과를 반환한다")
        void dealRank_empty() throws Exception {
            respond("/api/domestic/market/trend/trendForeignOrg", "{}");

            assertThat(json(client.getDealRank(DealRankMarket.values()[0], InvestorType.values()[0],
                    DealType.SELL, PeriodType.values()[0]))).contains("[]");
        }
    }
}
