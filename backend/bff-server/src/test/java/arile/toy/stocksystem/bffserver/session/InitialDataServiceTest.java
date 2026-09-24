package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.service.AccountCalculator;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import arile.toy.stocksystem.bffserver.chart.repository.ChartSnapshotRepository;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerBidAskPriceRepository;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerTradePriceRepository;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import arile.toy.stocksystem.bffserver.portfolio.service.PortfolioCalculator;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.repository.StockDetailSnapshotRepository;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class InitialDataServiceTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";

    @Mock private AccountCalculator accountCalculator;
    @Mock private PortfolioCalculator portfolioCalculator;
    @Mock private BffServerOrderResponseRepository bffServerOrderResponseRepository;
    @Mock private BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;
    @Mock private BffServerTradePriceRepository bffServerTradePriceRepository;
    @Mock private BffServerBidAskPriceRepository bffServerBidAskPriceRepository;
    @Mock private StockDetailSnapshotRepository stockDetailSnapshotRepository;
    @Mock private ChartSnapshotRepository chartSnapshotRepository;
    @Mock private BffServerTrailingStopResponseRepository bffServerTrailingStopResponseRepository;
    @Mock private BffServerOtocoResponseRepository bffServerOtocoResponseRepository;
    @Mock private BffServerAlertResponseRepository bffServerAlertResponseRepository;

    @InjectMocks
    private InitialDataService service;

    private static RedisConnectionFailureException redisDown() {
        return new RedisConnectionFailureException("redis down");
    }

    // ===================== 계좌 · 포트폴리오 =====================

    @Nested
    @DisplayName("계좌")
    class Account {

        @Test
        @DisplayName("계산한 계좌 정보를 반환한다")
        void found() {
            AccountResponse response = mock(AccountResponse.class);
            given(accountCalculator.calculate(USERNAME)).willReturn(response);

            assertThat(service.getAccountData(USERNAME)).containsSame(response);
        }

        @Test
        @DisplayName("계좌가 없으면 빈 결과를 반환한다 (예: 가입 직후 계좌 생성 전)")
        void accountNotFound() {
            given(accountCalculator.calculate(USERNAME)).willThrow(mock(RedisAccountNotFoundException.class));

            assertThat(service.getAccountData(USERNAME)).isEmpty();
        }

        @Test
        @DisplayName("그 밖의 계산 오류도 예외를 던지지 않고 빈 결과를 반환한다")
        void unexpectedError() {
            given(accountCalculator.calculate(USERNAME)).willThrow(new IllegalStateException("boom"));

            assertThat(service.getAccountData(USERNAME)).isEmpty();
        }
    }

    @Nested
    @DisplayName("포트폴리오")
    class Portfolio {

        @Test
        @DisplayName("계산한 포트폴리오를 반환한다")
        void found() {
            PortfolioResponse response = new PortfolioResponse(USERNAME, 1_000_000L, 1_000_000L, 100.0, List.of());
            given(portfolioCalculator.calculate(USERNAME)).willReturn(response);

            assertThat(service.getPortfolioData(USERNAME)).containsSame(response);
        }

        @Test
        @DisplayName("계좌가 없으면 빈 결과를 반환한다")
        void accountNotFound() {
            given(portfolioCalculator.calculate(USERNAME)).willThrow(mock(RedisAccountNotFoundException.class));

            assertThat(service.getPortfolioData(USERNAME)).isEmpty();
        }

        @Test
        @DisplayName("그 밖의 계산 오류도 예외를 던지지 않고 빈 결과를 반환한다")
        void unexpectedError() {
            given(portfolioCalculator.calculate(USERNAME)).willThrow(new IllegalStateException("boom"));

            assertThat(service.getPortfolioData(USERNAME)).isEmpty();
        }
    }

    // ===================== 사용자 주문 목록 =====================

    @Nested
    @DisplayName("사용자 주문 목록")
    class UserLists {

        @Test
        @DisplayName("미체결 주문·자동 주문·트레일링 스탑·OTOCO·알림 목록을 사용자 기준으로 조회해 반환한다")
        void found() {
            given(bffServerOrderResponseRepository.findAll(USERNAME)).willReturn(List.of());
            given(bffServerAutoOrderResponseRepository.findAll(USERNAME)).willReturn(List.of());
            given(bffServerTrailingStopResponseRepository.findAll(USERNAME)).willReturn(List.of());
            given(bffServerOtocoResponseRepository.findAll(USERNAME)).willReturn(List.of());
            given(bffServerAlertResponseRepository.findAll(USERNAME)).willReturn(List.of());

            assertThat(service.getOrderData(USERNAME)).contains(List.of());
            assertThat(service.getAutoOrderData(USERNAME)).contains(List.of());
            assertThat(service.getTrailingStopData(USERNAME)).contains(List.of());
            assertThat(service.getOtocoData(USERNAME)).contains(List.of());
            assertThat(service.getAlertData(USERNAME)).contains(List.of());
        }

        @Test
        @DisplayName("저장소가 null을 반환하면 빈 결과를 반환한다")
        void nullResult() {
            given(bffServerOrderResponseRepository.findAll(USERNAME)).willReturn(null);
            given(bffServerAutoOrderResponseRepository.findAll(USERNAME)).willReturn(null);
            given(bffServerTrailingStopResponseRepository.findAll(USERNAME)).willReturn(null);
            given(bffServerOtocoResponseRepository.findAll(USERNAME)).willReturn(null);
            given(bffServerAlertResponseRepository.findAll(USERNAME)).willReturn(null);

            assertThat(service.getOrderData(USERNAME)).isEmpty();
            assertThat(service.getAutoOrderData(USERNAME)).isEmpty();
            assertThat(service.getTrailingStopData(USERNAME)).isEmpty();
            assertThat(service.getOtocoData(USERNAME)).isEmpty();
            assertThat(service.getAlertData(USERNAME)).isEmpty();
        }

        @Test
        @DisplayName("조회 중 오류가 나도 예외를 던지지 않고 빈 결과를 반환한다")
        void error() {
            given(bffServerOrderResponseRepository.findAll(USERNAME)).willThrow(redisDown());
            given(bffServerAutoOrderResponseRepository.findAll(USERNAME)).willThrow(redisDown());
            given(bffServerTrailingStopResponseRepository.findAll(USERNAME)).willThrow(redisDown());
            given(bffServerOtocoResponseRepository.findAll(USERNAME)).willThrow(redisDown());
            given(bffServerAlertResponseRepository.findAll(USERNAME)).willThrow(redisDown());

            assertThat(service.getOrderData(USERNAME)).isEmpty();
            assertThat(service.getAutoOrderData(USERNAME)).isEmpty();
            assertThat(service.getTrailingStopData(USERNAME)).isEmpty();
            assertThat(service.getOtocoData(USERNAME)).isEmpty();
            assertThat(service.getAlertData(USERNAME)).isEmpty();
        }
    }

    // ===================== 종목 데이터 =====================

    @Nested
    @DisplayName("종목 데이터")
    class StockData {

        @Test
        @DisplayName("호가·체결가·종목 상세·일봉·분봉을 종목코드 기준으로 조회해 반환한다")
        void found() {
            BffServerBidAskPriceTickMessage bidAsk = mock(BffServerBidAskPriceTickMessage.class);
            BffServerTradePriceTickMessage trade = mock(BffServerTradePriceTickMessage.class);
            StockDetailTickMessage detail = mock(StockDetailTickMessage.class);
            given(bffServerBidAskPriceRepository.findByStockCode(STOCK_CODE)).willReturn(bidAsk);
            given(bffServerTradePriceRepository.findByStockCode(STOCK_CODE)).willReturn(trade);
            given(stockDetailSnapshotRepository.getLatest(STOCK_CODE)).willReturn(detail);
            given(chartSnapshotRepository.getDaily(STOCK_CODE)).willReturn(List.of());
            given(chartSnapshotRepository.getMinute(STOCK_CODE)).willReturn(List.of());

            assertThat(service.getBidAskPriceData(STOCK_CODE)).containsSame(bidAsk);
            assertThat(service.getTradePriceData(STOCK_CODE)).containsSame(trade);
            assertThat(service.getStockDetailData(STOCK_CODE)).containsSame(detail);
            assertThat(service.getDailyChartData(STOCK_CODE)).contains(List.of());
            assertThat(service.getMinuteChartData(STOCK_CODE)).contains(List.of());
        }

        @Test
        @DisplayName("데이터가 아직 없으면(장 시작 전·수집 전) 빈 결과를 반환한다")
        void notYetCollected() {
            given(bffServerBidAskPriceRepository.findByStockCode(STOCK_CODE)).willReturn(null);
            given(bffServerTradePriceRepository.findByStockCode(STOCK_CODE)).willReturn(null);
            given(stockDetailSnapshotRepository.getLatest(STOCK_CODE)).willReturn(null);
            given(chartSnapshotRepository.getDaily(STOCK_CODE)).willReturn(null);
            given(chartSnapshotRepository.getMinute(STOCK_CODE)).willReturn(null);

            assertThat(service.getBidAskPriceData(STOCK_CODE)).isEmpty();
            assertThat(service.getTradePriceData(STOCK_CODE)).isEmpty();
            assertThat(service.getStockDetailData(STOCK_CODE)).isEmpty();
            assertThat(service.getDailyChartData(STOCK_CODE)).isEmpty();
            assertThat(service.getMinuteChartData(STOCK_CODE)).isEmpty();
        }

        @Test
        @DisplayName("조회 중 오류가 나도 예외를 던지지 않고 빈 결과를 반환한다 (다른 스냅샷 전송에 영향 없음)")
        void error() {
            given(bffServerBidAskPriceRepository.findByStockCode(STOCK_CODE)).willThrow(redisDown());
            given(bffServerTradePriceRepository.findByStockCode(STOCK_CODE)).willThrow(redisDown());
            given(stockDetailSnapshotRepository.getLatest(STOCK_CODE)).willThrow(redisDown());
            given(chartSnapshotRepository.getDaily(STOCK_CODE)).willThrow(redisDown());
            given(chartSnapshotRepository.getMinute(STOCK_CODE)).willThrow(redisDown());

            assertThat(service.getBidAskPriceData(STOCK_CODE)).isEmpty();
            assertThat(service.getTradePriceData(STOCK_CODE)).isEmpty();
            assertThat(service.getStockDetailData(STOCK_CODE)).isEmpty();
            assertThat(service.getDailyChartData(STOCK_CODE)).isEmpty();
            assertThat(service.getMinuteChartData(STOCK_CODE)).isEmpty();
        }
    }
}
