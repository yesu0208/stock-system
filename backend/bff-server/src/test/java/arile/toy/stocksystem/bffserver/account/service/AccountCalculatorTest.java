package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.LeveragePositionInfo;
import arile.toy.stocksystem.bffserver.account.dto.LeveragePositionView;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerStockSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountCalculatorTest {

    private static final String USERNAME = "user1";
    private static final long INITIAL_BALANCE = 2_000_000L;

    @Mock private AccountPullService accountPullService;
    @Mock private BffServerStockSummaryRepository stockSummaryRepository;

    @InjectMocks
    private AccountCalculator calculator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(calculator, "initialBalance", INITIAL_BALANCE);
    }

    private static BffServerStockSummaryTickMessage summary(String stockCode, int curPrice) {
        return new BffServerStockSummaryTickMessage(stockCode, curPrice, 0);
    }

    private static StockInfo spot005930() {
        return new StockInfo(10, 10, 700_000L, 700_105L);
    }

    private static LeveragePositionInfo leverage000660() {
        return new LeveragePositionInfo(10, 10, 1_000_000L, 1_000_150L, 500_000L, "NORMAL");
    }

    private void givenSnapshot(Map<String, StockInfo> stocks, Map<String, LeveragePositionInfo> leverage) {
        given(accountPullService.getAccountMessage(USERNAME)).willReturn(
                AccountSnapshot.of(1_000_000L, 200_000L, stocks, leverage, "NORMAL", "NORMAL"));
    }

    // ===================== 전체 시나리오 =====================

    @Nested
    @DisplayName("현물 + 레버리지 보유 계좌")
    class FullAccount {

        private AccountResponse response;

        @BeforeEach
        void calculate() {
            givenSnapshot(Map.of("005930", spot005930()), Map.of("000660:X2", leverage000660()));
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(summary("005930", 80_000));
            given(stockSummaryRepository.findByStockCode("000660")).willReturn(summary("000660", 120_000));

            response = calculator.calculate(USERNAME);
        }

        @Test
        @DisplayName("현금은 가용 현금과 예약 현금을 합하고, 스냅샷 값과 상태는 그대로 전달한다")
        void cash() {
            assertThat(response.username()).isEqualTo(USERNAME);
            assertThat(response.totalCash()).isEqualTo(1_200_000L);
            assertThat(response.availableCash()).isEqualTo(1_000_000L);
            assertThat(response.reservedCash()).isEqualTo(200_000L);
            assertThat(response.stocks()).containsKey("005930");
            assertThat(response.marginStatus()).isEqualTo("NORMAL");
            assertThat(response.accountStatus()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("현물: 평가금액·매입금액과 종목별 현재가·손익(매도 비용 차감)·수익률(매입원금 기준)을 계산한다")
        void spot() {
            assertThat(response.stockValue()).isEqualTo(800_000L);
            assertThat(response.buyValue()).isEqualTo(700_000L);
            assertThat(response.currentPrices()).containsEntry("005930", 80_000);
            assertThat(response.profitAmounts()).containsEntry("005930", 98_175L);
            assertThat(response.profitRates().get("005930"))
                    .isCloseTo(98_175 * 100.0 / 700_105, within(1e-9));
        }

        @Test
        @DisplayName("레버리지: 순자산(평가금액-대출금)·대출금·매입금액 합계를 계산한다")
        void leverageTotals() {
            assertThat(response.leverageNetValue()).isEqualTo(700_000L);
            assertThat(response.leverageLoanTotal()).isEqualTo(500_000L);
            assertThat(response.leveragePurchaseTotal()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("레버리지 포지션 뷰: 키를 종목코드·배율로 나누고 손익·수익률(투입원금 기준)·증거금 정보를 계산한다")
        void leverageView() {
            assertThat(response.leveragePositions()).hasSize(1);
            LeveragePositionView view = response.leveragePositions().get(0);

            assertThat(view.stockCode()).isEqualTo("000660");
            assertThat(view.leverageRatio()).isEqualTo("X2");
            assertThat(view.quantity()).isEqualTo(10);
            assertThat(view.currentPrice()).isEqualTo(120_000);
            assertThat(view.evaluationAmount()).isEqualTo(1_200_000L);
            assertThat(view.netValue()).isEqualTo(700_000L);
            assertThat(view.profitAmount()).isEqualTo(197_270L);
            assertThat(view.profitRate()).isCloseTo(197_270 * 100.0 / 500_150, within(1e-9));
            assertThat(view.marginStatus()).isEqualTo("NORMAL");
            assertThat(view.initialMargin()).isEqualTo(500_000L);
            assertThat(view.maintenanceMargin()).isEqualTo(700_000L);
            assertThat(view.maintenancePrice()).isEqualTo(70_000L);
        }

        @Test
        @DisplayName("총자산 = 현금 + 현물 평가금액 + 레버리지 순자산, 총손익·총수익률은 현물과 레버리지를 합산한다")
        void totals() {
            assertThat(response.totalValue()).isEqualTo(2_700_000L);
            assertThat(response.totalProfit()).isEqualTo(295_445L);
            assertThat(response.totalProfitRate())
                    .isCloseTo(295_445 * 100.0 / (700_105 + 500_150), within(1e-9));
        }

        @Test
        @DisplayName("누적 손익·수익률은 초기 자본 대비로 계산한다")
        void accumulated() {
            assertThat(response.accumulatedProfit()).isEqualTo(700_000L);
            assertThat(response.accumulatedProfitRate()).isCloseTo(35.0, within(1e-9));
        }
    }

    // ===================== 시세 조회 =====================

    @Nested
    @DisplayName("시세 조회")
    class Prices {

        @Test
        @DisplayName("현물과 같은 종목의 레버리지 포지션은 현물에서 조회한 현재가를 재사용한다 (중복 조회 없음)")
        void reusesSpotPrice() {
            givenSnapshot(Map.of("005930", spot005930()), Map.of("005930:X2", leverage000660()));
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(summary("005930", 80_000));

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.leveragePositions().get(0).currentPrice()).isEqualTo(80_000);
            verify(stockSummaryRepository, times(1)).findByStockCode("005930");
        }

        @Test
        @DisplayName("현재가가 없는 현물 종목은 평가·손익에서 제외한다")
        void spotWithoutPrice_skipped() {
            givenSnapshot(Map.of("005930", spot005930()), Map.of());
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(null);

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.stockValue()).isZero();
            assertThat(response.buyValue()).isZero();
            assertThat(response.currentPrices()).isEmpty();
            assertThat(response.profitAmounts()).isEmpty();
            assertThat(response.totalValue()).isEqualTo(1_200_000L);
        }

        @Test
        @DisplayName("현재가가 없는 레버리지 포지션은 순자산·손익·뷰에서 제외한다")
        void leverageWithoutPrice_skipped() {
            givenSnapshot(Map.of(), Map.of("000660:X2", leverage000660()));
            given(stockSummaryRepository.findByStockCode("000660")).willReturn(null);

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.leverageNetValue()).isZero();
            assertThat(response.leverageLoanTotal()).isZero();
            assertThat(response.leveragePositions()).isEmpty();
        }
    }

    // ===================== 경계·예외 입력 =====================

    @Nested
    @DisplayName("경계·예외 입력")
    class EdgeCases {

        @Test
        @DisplayName("레버리지 포지션 맵이 null이면 레버리지 값은 모두 0이고 뷰는 빈 목록이다")
        void nullLeverage() {
            givenSnapshot(Map.of(), null);

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.leverageNetValue()).isZero();
            assertThat(response.leveragePurchaseTotal()).isZero();
            assertThat(response.leveragePositions()).isEmpty();
        }

        @Test
        @DisplayName("'종목코드:배율' 형식이 아닌 레버리지 키는 건너뛴다")
        void malformedKey_skipped() {
            givenSnapshot(Map.of(), Map.of("000660", leverage000660()));

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.leveragePositions()).isEmpty();
        }

        @Test
        @DisplayName("보유 종목이 없으면 총수익률은 0이고, 총자산은 현금뿐이다")
        void noHoldings() {
            givenSnapshot(Map.of(), Map.of());

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.totalValue()).isEqualTo(1_200_000L);
            assertThat(response.totalProfit()).isZero();
            assertThat(response.totalProfitRate()).isZero();
            assertThat(response.accumulatedProfit()).isEqualTo(-800_000L);
            assertThat(response.accumulatedProfitRate()).isCloseTo(-40.0, within(1e-9));
        }

        @Test
        @DisplayName("매입원금이 0인 현물은 수익률 0, 투입원금·수량이 0인 레버리지는 수익률·유지증거금 기준가 0이다")
        void zeroDenominators() {
            givenSnapshot(
                    Map.of("005930", new StockInfo(0, 0, 0L, 0L)),
                    Map.of("000660:X2", new LeveragePositionInfo(0, 0, 500_000L, 500_000L, 500_000L, "NORMAL")));
            given(stockSummaryRepository.findByStockCode("005930")).willReturn(summary("005930", 80_000));
            given(stockSummaryRepository.findByStockCode("000660")).willReturn(summary("000660", 120_000));

            AccountResponse response = calculator.calculate(USERNAME);

            assertThat(response.profitRates().get("005930")).isZero();
            LeveragePositionView view = response.leveragePositions().get(0);
            assertThat(view.profitRate()).isZero();
            assertThat(view.maintenancePrice()).isZero();
        }
    }
}
