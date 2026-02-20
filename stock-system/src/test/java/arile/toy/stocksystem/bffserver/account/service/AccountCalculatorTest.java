package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerStockSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountCalculatorTest {

    @Mock
    private AccountPullService accountPullService;

    @Mock
    private BffServerStockSummaryRepository stockSummaryRepository;

    @InjectMocks
    private AccountCalculator accountCalculator;

    @Test
    @DisplayName("계좌 계산 정상 케이스")
    void givenAccountSnapshot_whenCalculate_thenReturnsCorrectResponse() {
        // given
        String username = "user1";

        Map<String, StockInfo> stocks = Map.of(
                "005930", new StockInfo(10, 10, 50000),
                "000660", new StockInfo(5, 5, 100000)
        );

        AccountSnapshot snapshot = new AccountSnapshot(
                1_000_000L, 500_000L, stocks
        );

        given(accountPullService.getAccountMessage(username)).willReturn(snapshot);
        given(stockSummaryRepository.findByStockCode("005930"))
                .willReturn(new BffServerStockSummaryTickMessage("005930", 60_000, 500));
        given(stockSummaryRepository.findByStockCode("000660"))
                .willReturn(new BffServerStockSummaryTickMessage("000660", 90_000, 500));

        // when
        AccountResponse response = accountCalculator.calculate(username);

        // then
        assertEquals(username, response.username());

        long expectedStockValue = 10 * 60_000L + 5 * 90_000L;
        long expectedTotalCash = 1_000_000L + 500_000L;
        long expectedTotalValue = expectedTotalCash + expectedStockValue;
        assertEquals(expectedTotalValue, response.totalValue());

        assertEquals(expectedTotalCash, response.totalCash());

        assertEquals((60_000 - 50_000) * 10L, response.profitAmounts().get("005930"));
        assertEquals((90_000 - 100_000) * 5L, response.profitAmounts().get("000660"));

        assertEquals((60_000 - 50_000) * 100.0 / 50_000, response.profitRates().get("005930"));
        assertEquals((90_000 - 100_000) * 100.0 / 100_000, response.profitRates().get("000660"));

        assertEquals(60_000, response.currentPrices().get("005930"));
        assertEquals(90_000, response.currentPrices().get("000660"));
    }

    @Test
    @DisplayName("주식 요약이 없으면 warning 로그만 남기고 계산 제외")
    void givenNoStockSummary_whenCalculate_thenSkipsStock() {
        // given
        String username = "user2";
        Map<String, StockInfo> stocks = Map.of(
                "123456", new StockInfo(5, 5, 10_000)
        );
        AccountSnapshot snapshot = new AccountSnapshot(100_000L, 50_000L, stocks);

        given(accountPullService.getAccountMessage(username)).willReturn(snapshot);
        given(stockSummaryRepository.findByStockCode("123456")).willReturn(null);

        // when
        AccountResponse response = accountCalculator.calculate(username);

        // then
        long expectedTotalCash = 100_000L + 50_000L;
        assertEquals(expectedTotalCash, response.totalValue());
        assertEquals(0, response.stockValue());
        assertEquals(0, response.buyValue());
        assertTrue(response.profitAmounts().isEmpty());
        assertTrue(response.profitRates().isEmpty());
        assertTrue(response.currentPrices().isEmpty());
    }
}
