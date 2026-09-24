package arile.toy.stocksystem.bffserver.stockinfo.service;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StockInfoServiceTest {

    private static final Duration TTL = Duration.ofSeconds(60);
    private static final String TREND_KEY = "stock:investor:trend:KOSPI:DAY:1";

    private NaverStockCrawlerClient crawler;
    private ValueOperations<String, String> valueOps;
    private StockInfoService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        crawler = mock(NaverStockCrawlerClient.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        service = new StockInfoService(crawler, redisTemplate, new ObjectMapper());
    }

    private static TrendResponse trend(boolean hasNext) {
        return new TrendResponse(List.of(), hasNext);
    }

    // ===================== 캐시 동작 (투자자 동향으로 대표 검증) =====================

    @Nested
    @DisplayName("캐시 동작")
    class Caching {

        @Test
        @DisplayName("캐시가 없으면 크롤링하고 60초간 캐시한다")
        void cacheMiss() {
            given(crawler.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).willReturn(trend(true));

            assertThat(service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).isEqualTo(trend(true));

            verify(valueOps).set(eq(TREND_KEY), anyString(), eq(TTL));
        }

        @Test
        @DisplayName("캐시가 있으면 크롤링하지 않는다")
        void cacheHit() {
            given(valueOps.get(TREND_KEY)).willReturn("{\"data\": [], \"hasNext\": true}");

            assertThat(service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1).hasNext()).isTrue();

            verifyNoInteractions(crawler);
        }

        @Test
        @DisplayName("캐시가 깨져 있으면 크롤링으로 대체한다")
        void brokenCache() {
            given(valueOps.get(TREND_KEY)).willReturn("{broken");
            given(crawler.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).willReturn(trend(false));

            assertThat(service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).isEqualTo(trend(false));
        }

        @Test
        @DisplayName("캐시 조회가 실패해도(Redis 장애) 크롤링으로 대체한다")
        void cacheReadFails() {
            given(valueOps.get(TREND_KEY)).willThrow(new RedisConnectionFailureException("down"));
            given(crawler.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).willReturn(trend(false));

            assertThat(service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).isEqualTo(trend(false));
        }

        @Test
        @DisplayName("캐시 저장이 실패해도 결과는 그대로 반환한다")
        void cacheWriteFails() {
            given(crawler.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).willReturn(trend(false));
            willThrow(new RedisConnectionFailureException("down"))
                    .given(valueOps).set(anyString(), anyString(), eq(TTL));

            assertThat(service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1)).isEqualTo(trend(false));
        }

        @Test
        @DisplayName("네이버 장애(크롤러의 IllegalStateException)는 503 ClientErrorException으로 바꾼다")
        void naverUnavailable_503() {
            given(crawler.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1))
                    .willThrow(new IllegalStateException("네이버 투자자별 매매동향 크롤링 실패"));

            assertThatThrownBy(() -> service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1))
                    .isInstanceOfSatisfying(ClientErrorException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                        assertThat(e.getMessage()).isEqualTo("시세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
                    });
        }

        @Test
        @DisplayName("크롤러의 다른 예외(응답 변환 버그 등)는 그대로 전파해 서버 오류로 드러나게 한다")
        void unexpectedError_propagates() {
            given(crawler.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1))
                    .willThrow(new NullPointerException("mapping bug"));

            assertThatThrownBy(() -> service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, 1))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ===================== 페이지 검증 =====================

    @ParameterizedTest(name = "page={0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("페이지가 1 미만이면 네이버를 호출하지 않고 400으로 거절한다")
    void invalidPage_400(int page) {
        assertThatThrownBy(() -> service.getInvestorTrend(MarketType.KOSPI, TrendType.DAY, page))
                .isInstanceOfSatisfying(ClientErrorException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.getForeignInstitutionTrades("005930", page))
                .isInstanceOf(ClientErrorException.class);

        verifyNoInteractions(crawler, valueOps);
    }

    // ===================== 메서드별 캐시 키 =====================

    /** (설명, 서비스 호출, 기대 캐시 키, 크롤러 호출 확인) */
    record KeyCase(String name, Consumer<StockInfoService> call, String key,
                   Consumer<NaverStockCrawlerClient> crawlerCall) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<KeyCase> keyCases() {
        return Stream.of(
                new KeyCase("종목 정보", s -> s.getStockInfo(" 005930 "), "stock:info:005930",
                        c -> c.getStockInfo(" 005930 ")),
                new KeyCase("외국인·기관 매매", s -> s.getForeignInstitutionTrades("005930", 2), "stock:foreign:005930:2",
                        c -> c.getForeignInstitutionTrades("005930", 2)),
                new KeyCase("인기 종목", StockInfoService::getPopularStocks, "stock:popular",
                        NaverStockCrawlerClient::getPopularStocks),
                new KeyCase("업종 목록", StockInfoService::getAllUpjongs, "stock:upjong:all",
                        NaverStockCrawlerClient::getAllUpjongs),
                new KeyCase("업종별 종목", s -> s.getUpjongStocks("278"), "stock:upjong:stocks:278",
                        c -> c.getUpjongStocks("278")),
                new KeyCase("종목 상세 부가정보", s -> s.getStockDetailExtra("005930"), "stock:detail:extra:005930",
                        c -> c.getStockDetailExtra("005930")),
                new KeyCase("투자자 동향", s -> s.getInvestorTrend(MarketType.KOSDAQ, TrendType.TIME, 3),
                        "stock:investor:trend:KOSDAQ:TIME:3",
                        c -> c.getInvestorTrend(MarketType.KOSDAQ, TrendType.TIME, 3)),
                new KeyCase("수급 순위",
                        s -> s.getDealRank(DealRankMarket.values()[0], InvestorType.values()[0],
                                DealType.BUY, PeriodType.values()[0]),
                        "stock:deal-rank:" + DealRankMarket.values()[0].name() + ":" + InvestorType.values()[0].name()
                                + ":BUY:" + PeriodType.values()[0].name(),
                        c -> c.getDealRank(DealRankMarket.values()[0], InvestorType.values()[0],
                                DealType.BUY, PeriodType.values()[0]))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    @DisplayName("메서드마다 자기 캐시 키를 조회하고, 캐시가 없으면 해당 크롤러를 호출한다")
    void cacheKeyAndCrawler(KeyCase keyCase) {
        try {
            keyCase.call().accept(service);
        } catch (RuntimeException ignored) {
            // 목 크롤러가 null을 돌려줘 후처리에서 실패할 수 있음 (키·호출 확인이 목적)
        }

        verify(valueOps).get(keyCase.key());
        keyCase.crawlerCall().accept(verify(crawler));
    }

    // ===================== 업종명 =====================

    @Test
    @DisplayName("업종별 종목: 업종 목록에서 업종명을 찾아 함께 담는다")
    void upjongName() {
        UpjongInfo semiconductor = mock(UpjongInfo.class);
        given(semiconductor.no()).willReturn("278");
        given(semiconductor.name()).willReturn("반도체");
        given(crawler.getAllUpjongs()).willReturn(new UpjongResponse(List.of(semiconductor)));
        given(crawler.getUpjongStocks("278")).willReturn(List.of());

        UpjongStockResponse response = service.getUpjongStocks("278");

        assertThat(response.upjongName()).isEqualTo("반도체");
    }

    @Test
    @DisplayName("업종별 종목: 업종 목록 조회가 실패해도 업종명만 비우고 종목 목록은 반환한다")
    void upjongName_fallback() {
        given(crawler.getAllUpjongs()).willThrow(new IllegalStateException("naver down"));
        given(crawler.getUpjongStocks("278")).willReturn(List.of());

        UpjongStockResponse response = service.getUpjongStocks("278");

        assertThat(response.upjongName()).isEmpty();
    }
}
