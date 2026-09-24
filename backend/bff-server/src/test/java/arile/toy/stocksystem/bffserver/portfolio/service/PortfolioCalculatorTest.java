package arile.toy.stocksystem.bffserver.portfolio.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.LeveragePositionInfo;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.account.service.AccountPullService;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerStockSummaryRepository;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioSectorItem;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioStockItem;
import arile.toy.stocksystem.bffserver.sector.SectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioCalculatorTest {

    private static final String USERNAME = "user1";
    private static final String SEMICONDUCTOR = "반도체";

    @Mock private AccountPullService accountPullService;
    @Mock private BffServerStockSummaryRepository stockSummaryRepository;
    @Mock private SectorRegistry sectorRegistry;

    @InjectMocks
    private PortfolioCalculator calculator;

    private static BffServerStockSummaryTickMessage summary(String stockCode, int curPrice) {
        return new BffServerStockSummaryTickMessage(stockCode, curPrice, 0);
    }

    private void givenSnapshot(long availableCash, long reservedCash,
                               Map<String, StockInfo> stocks, Map<String, LeveragePositionInfo> leverage) {
        given(accountPullService.getAccountMessage(USERNAME)).willReturn(
                AccountSnapshot.of(availableCash, reservedCash, stocks, leverage, "NORMAL", "NORMAL"));
    }

    private static PortfolioStockItem stockOf(PortfolioSectorItem sector, String stockCode) {
        return sector.stocks().stream()
                .filter(item -> item.stockCode().equals(stockCode))
                .findFirst()
                .orElseThrow();
    }

    // ===================== 전체 시나리오 =====================

    @Nested
    @DisplayName("여러 업종의 현물·레버리지를 보유한 계좌")
    class FullPortfolio {

        private PortfolioResponse response;

        @BeforeEach
        void calculate() {
            Map<String, StockInfo> stocks = new LinkedHashMap<>();
            stocks.put("005930", new StockInfo(10, 10, 700_000L, 700_105L));
            stocks.put("035420", new StockInfo(2, 2, 380_000L, 380_057L));

            Map<String, LeveragePositionInfo> leverage = new LinkedHashMap<>();
            leverage.put("005930:X2", new LeveragePositionInfo(5, 5, 350_000L, 350_050L, 175_000L, "NORMAL"));
            leverage.put("000660:X1_5", new LeveragePositionInfo(10, 10, 1_000_000L, 1_000_150L, 500_000L, "NORMAL"));

            givenSnapshot(1_000_000L, 200_000L, stocks, leverage);
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(summary("005930", 80_000));
            given(stockSummaryRepository.findByStockCode("035420")).willReturn(summary("035420", 200_000));
            given(stockSummaryRepository.findByStockCode("000660")).willReturn(summary("000660", 120_000));
            given(sectorRegistry.resolveSector("005930")).willReturn(SEMICONDUCTOR);
            given(sectorRegistry.resolveSector("000660")).willReturn(SEMICONDUCTOR);
            given(sectorRegistry.resolveSector("035420")).willReturn(SectorRegistry.UNCLASSIFIED_SECTOR);

            response = calculator.calculate(USERNAME);
        }

        @Test
        @DisplayName("총자산은 현금 + 현물 평가금액 + 레버리지 평가금액(대출금 미차감)이고, 현금 비중을 계산한다")
        void totals() {
            assertThat(response.username()).isEqualTo(USERNAME);
            assertThat(response.cashValue()).isEqualTo(1_200_000L);
            assertThat(response.totalAssetValue()).isEqualTo(4_000_000L);
            assertThat(response.cashRatio()).isCloseTo(30.0, within(1e-9));
        }

        @Test
        @DisplayName("업종별로 묶어 평가금액이 큰 순으로 정렬하고, 전체 자산 대비 비중을 계산한다")
        void sectors() {
            assertThat(response.sectors()).extracting(PortfolioSectorItem::sector)
                    .containsExactly(SEMICONDUCTOR, SectorRegistry.UNCLASSIFIED_SECTOR);

            PortfolioSectorItem semiconductor = response.sectors().get(0);
            assertThat(semiconductor.evaluationAmount()).isEqualTo(2_400_000L);
            assertThat(semiconductor.ratioInTotal()).isCloseTo(60.0, within(1e-9));
            assertThat(semiconductor.stocks()).hasSize(2);

            PortfolioSectorItem unclassified = response.sectors().get(1);
            assertThat(unclassified.evaluationAmount()).isEqualTo(400_000L);
            assertThat(unclassified.ratioInTotal()).isCloseTo(10.0, within(1e-9));
        }

        @Test
        @DisplayName("같은 종목의 현물과 레버리지는 한 항목으로 합산하고, 각각의 손익·수익률도 따로 보여준다")
        void mergedStock() {
            PortfolioStockItem item = stockOf(response.sectors().get(0), "005930");

            assertThat(item.spotAmount()).isEqualTo(800_000L);
            assertThat(item.leverageAmount()).isEqualTo(400_000L);
            assertThat(item.totalAmount()).isEqualTo(1_200_000L);
            assertThat(item.ratioInSector()).isCloseTo(50.0, within(1e-9));
            assertThat(item.ratioInTotal()).isCloseTo(30.0, within(1e-9));

            assertThat(item.spotBuyAmount()).isEqualTo(700_000L);
            assertThat(item.spotProfitAmount()).isEqualTo(98_175L);
            assertThat(item.spotProfitRate()).isCloseTo(98_175 * 100.0 / 700_105, within(1e-9));

            assertThat(item.leverageBuyAmount()).isEqualTo(350_000L);
            assertThat(item.leverageEquityAmount()).isEqualTo(175_000L);
            assertThat(item.leverageProfitAmount()).isEqualTo(49_090L);
            assertThat(item.leverageProfitRate()).isCloseTo(49_090 * 100.0 / 175_050, within(1e-9));

            assertThat(item.profitAmount()).isEqualTo(147_265L);
            assertThat(item.profitRate()).isCloseTo(147_265 * 100.0 / (700_105 + 175_050), within(1e-9));
        }

        @Test
        @DisplayName("레버리지만 보유한 종목은 현물 값이 0이고 현물 수익률도 0이다")
        void leverageOnlyStock() {
            PortfolioStockItem item = stockOf(response.sectors().get(0), "000660");

            assertThat(item.spotAmount()).isZero();
            assertThat(item.leverageAmount()).isEqualTo(1_200_000L);
            assertThat(item.spotProfitRate()).isZero();
            assertThat(item.leverageProfitAmount()).isEqualTo(197_270L);
            assertThat(item.leverageProfitRate()).isCloseTo(197_270 * 100.0 / 500_150, within(1e-9));
        }

        @Test
        @DisplayName("현물만 보유한 종목은 레버리지 수익률이 0이고, 업종 안에서 비중 100%다")
        void spotOnlyStock() {
            PortfolioStockItem item = stockOf(response.sectors().get(1), "035420");

            assertThat(item.leverageAmount()).isZero();
            assertThat(item.leverageProfitRate()).isZero();
            assertThat(item.spotProfitAmount()).isEqualTo(19_083L);
            assertThat(item.ratioInSector()).isCloseTo(100.0, within(1e-9));
        }

        @Test
        @DisplayName("현물과 레버리지가 같은 종목이면 현재가를 한 번만 조회한다")
        void priceLookedUpOnce() {
            verify(stockSummaryRepository, times(1)).findByStockCode("005930");
        }
    }

    // ===================== 경계·예외 입력 =====================

    @Nested
    @DisplayName("경계·예외 입력")
    class EdgeCases {

        @Test
        @DisplayName("현재가가 없는 종목은 현물·레버리지 모두 제외한다")
        void noPrice_skipped() {
            givenSnapshot(1_000_000L, 0L,
                    Map.of("005930", new StockInfo(10, 10, 700_000L, 700_105L)),
                    Map.of("005930:X2", new LeveragePositionInfo(5, 5, 350_000L, 350_050L, 175_000L, "NORMAL")));
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(null);

            PortfolioResponse response = calculator.calculate(USERNAME);

            assertThat(response.sectors()).isEmpty();
            assertThat(response.totalAssetValue()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("'종목코드:배율' 형식이 아닌 레버리지 키는 건너뛴다")
        void malformedKey_skipped() {
            givenSnapshot(1_000_000L, 0L, Map.of(),
                    Map.of("000660", new LeveragePositionInfo(10, 10, 1_000_000L, 1_000_150L, 500_000L, "NORMAL")));

            assertThat(calculator.calculate(USERNAME).sectors()).isEmpty();
        }

        @Test
        @DisplayName("보유 종목 맵이 null이면 현금만으로 계산해 현금 비중이 100%다")
        void nullHoldings() {
            givenSnapshot(1_000_000L, 200_000L, null, null);

            PortfolioResponse response = calculator.calculate(USERNAME);

            assertThat(response.totalAssetValue()).isEqualTo(1_200_000L);
            assertThat(response.cashRatio()).isCloseTo(100.0, within(1e-9));
            assertThat(response.sectors()).isEmpty();
        }

        @Test
        @DisplayName("총자산이 0이면 비중은 0으로 계산한다 (0으로 나누지 않음)")
        void zeroAsset() {
            givenSnapshot(0L, 0L, Map.of(), Map.of());

            PortfolioResponse response = calculator.calculate(USERNAME);

            assertThat(response.totalAssetValue()).isZero();
            assertThat(response.cashRatio()).isZero();
        }

        @Test
        @DisplayName("매입원금이 0인 종목은 합산 수익률이 0이다")
        void zeroCost() {
            givenSnapshot(0L, 0L, Map.of("005930", new StockInfo(0, 0, 0L, 0L)), Map.of());
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(summary("005930", 80_000));
            given(sectorRegistry.resolveSector("005930")).willReturn(SEMICONDUCTOR);

            PortfolioStockItem item = calculator.calculate(USERNAME).sectors().get(0).stocks().get(0);

            assertThat(item.profitRate()).isZero();
            assertThat(item.spotProfitRate()).isZero();
            assertThat(item.ratioInSector()).isZero();
        }
    }
}
