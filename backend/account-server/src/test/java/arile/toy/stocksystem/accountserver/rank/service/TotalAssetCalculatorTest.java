package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.stockprice.dto.StockSummaryTickMessage;
import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import arile.toy.stocksystem.accountserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.accountserver.userstock.repository.UserStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TotalAssetCalculatorTest {

    private static final String USERNAME = "user1";

    @Mock private UserStockRepository userStockRepository;
    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private StockSummaryRedisRepository stockSummaryRedisRepository;

    @InjectMocks
    private TotalAssetCalculator calculator;

    private void givenPrice(String stockCode, Integer price) {
        given(stockSummaryRedisRepository.findByStockCode(stockCode))
                .willReturn(new StockSummaryTickMessage(stockCode, price, 0));
    }

    private static UserStockEntity stock(String stockCode, int quantity) {
        return UserStockEntity.of(USERNAME, stockCode, 0L, 0L, quantity);
    }

    /** X2, 매입 700,000 → 대출 350,000 */
    private static LeveragePositionEntity leverage(String stockCode, int quantity) {
        return LeveragePositionEntity.of(USERNAME, stockCode, LeverageRatio.X2, quantity, 700_000L, 700_100L);
    }

    @Test
    @DisplayName("보유 종목이 없으면 현금이 곧 총자산이다")
    void cashOnly() {
        given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of());
        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(List.of());

        assertThat(calculator.calculate(USERNAME, 1_000_000L)).isEqualTo(1_000_000L);
    }

    @Test
    @DisplayName("현금 + 현물 평가금액 + 레버리지 순자산(평가금액 - 대출금)을 합산한다")
    void sumsCashSpotAndLeverageNet() {
        given(userStockRepository.findByUsername(USERNAME))
                .willReturn(List.of(stock("005930", 10), stock("000660", 2)));
        given(leveragePositionRepository.findByUsername(USERNAME))
                .willReturn(List.of(leverage("035720", 10)));
        givenPrice("005930", 70_000); // 700,000
        givenPrice("000660", 150_000); // 300,000
        givenPrice("035720", 60_000); // 600,000 - 350,000 = 250,000

        long total = calculator.calculate(USERNAME, 100_000L);

        assertThat(total).isEqualTo(100_000L + 700_000L + 300_000L + 250_000L);
    }

    @Test
    @DisplayName("레버리지 평가금액이 대출금보다 작으면 음수 순자산이 총자산을 깎는다")
    void negativeLeverageNet_reducesTotal() {
        given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of());
        given(leveragePositionRepository.findByUsername(USERNAME))
                .willReturn(List.of(leverage("035720", 10)));
        givenPrice("035720", 30_000);   // 300,000 - 350,000 = -50,000

        assertThat(calculator.calculate(USERNAME, 100_000L)).isEqualTo(50_000L);
    }

    @Test
    @DisplayName("현금이 마이너스여도 그대로 합산한다")
    void negativeCash() {
        given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of(stock("005930", 1)));
        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(List.of());
        givenPrice("005930", 70_000);

        assertThat(calculator.calculate(USERNAME, -20_000L)).isEqualTo(50_000L);
    }

    @Test
    @DisplayName("시세가 없거나 현재가가 null인 종목은 현물·레버리지 모두 합산에서 제외한다")
    void skipsWhenNoPrice() {
        given(userStockRepository.findByUsername(USERNAME))
                .willReturn(List.of(stock("NOSUMMARY_SPOT", 10), stock("NULLPRICE_SPOT", 10)));
        given(leveragePositionRepository.findByUsername(USERNAME))
                .willReturn(List.of(leverage("NOSUMMARY_LEV", 10), leverage("NULLPRICE_LEV", 10)));
        given(stockSummaryRedisRepository.findByStockCode("NOSUMMARY_SPOT")).willReturn(null);
        givenPrice("NULLPRICE_SPOT", null);
        given(stockSummaryRedisRepository.findByStockCode("NOSUMMARY_LEV")).willReturn(null);
        givenPrice("NULLPRICE_LEV", null);

        assertThat(calculator.calculate(USERNAME, 100_000L)).isEqualTo(100_000L);
    }
}
