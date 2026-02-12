package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerStockSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountCalculator {

    private final AccountPullService accountPullService;
    private final BffServerStockSummaryRepository stockSummaryRepository;

    public AccountResponse calculate(String username) {
        AccountSnapshot snapshot = accountPullService.getAccountMessage(username);

        long totalCash = snapshot.availableCash() + snapshot.reservedCash();
        long stockValue = 0L;
        long buyValue = 0L;

        Map<String, Double> profitRates = new HashMap<>();
        Map<String, Long> profitAmounts = new HashMap<>();
        Map<String, Integer> currentPrices = new HashMap<>();

        for (var entry : snapshot.stocks().entrySet()) {
            String stockCode = entry.getKey();
            StockInfo stockInfo = entry.getValue();
            int quantity = stockInfo.quantity();
            int buyPrice = stockInfo.buyPrice();

            BffServerStockSummaryTickMessage summary = stockSummaryRepository.findByStockCode(stockCode);
            if (summary == null) {
                log.warn("No stock summary for {}", stockCode);
                continue;
            }
            int curPrice = summary.curPrice();

            currentPrices.put(stockCode, curPrice);

            long curStockValue = (long) quantity * curPrice;
            stockValue += curStockValue;

            long curBuyValue = (long) buyPrice * quantity;
            buyValue += curBuyValue;

            long profitAmount = (long) (curPrice - buyPrice) * quantity;
            double profitRate = (curPrice - buyPrice) * 100.0 / buyPrice;

            profitAmounts.put(stockCode, profitAmount);
            profitRates.put(stockCode, profitRate);
        }

        long totalValue = totalCash + stockValue;
        long totalProfit = stockValue - buyValue;
        double totalProfitRate = 0;
        if (buyValue != 0) totalProfitRate = totalProfit * 100.0 / buyValue;

        long accumulatedProfit = totalValue - 500000000;
        double accumulatedProfitRate = (double) (totalValue - 500000000) / 500000000 * 100;

        return AccountResponse.of(username, totalValue, totalCash, snapshot.availableCash(),
                snapshot.reservedCash(), stockValue, buyValue, totalProfit, totalProfitRate, accumulatedProfit, accumulatedProfitRate, snapshot.stocks(), profitRates, profitAmounts, currentPrices);
    }
}


