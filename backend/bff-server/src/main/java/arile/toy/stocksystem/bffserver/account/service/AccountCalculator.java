package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.*;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerStockSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountCalculator {

    private final AccountPullService accountPullService;
    private final BffServerStockSummaryRepository stockSummaryRepository;

    @Value("${account.initial-balance}")
    private long initialBalance;

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
            long totalAmount = stockInfo.totalAmount();

            BffServerStockSummaryTickMessage summary = stockSummaryRepository.findByStockCode(stockCode);
            if (summary == null) {
                log.warn("No stock summary for {}", stockCode);
                continue;
            }
            int curPrice = summary.curPrice();

            currentPrices.put(stockCode, curPrice);

            long curStockValue = (long) quantity * curPrice;
            stockValue += curStockValue;

            buyValue += totalAmount;

            long profitAmount = curStockValue - totalAmount;
            double profitRate = totalAmount == 0 ? 0 : profitAmount * 100.0 / totalAmount;

            profitAmounts.put(stockCode, profitAmount);
            profitRates.put(stockCode, profitRate);
        }

        var leverageResult = calculateLeverage(snapshot.leveragePositions(), currentPrices);

        long totalValue = totalCash + stockValue + leverageResult.netValue();
        long totalEquityValue = buyValue + leverageResult.equityTotal();
        long totalProfit = stockValue - buyValue + leverageResult.profitTotal();
        double totalProfitRate = totalEquityValue != 0 ? totalProfit * 100.0 / totalEquityValue : 0;

        long accumulatedProfit = totalValue - initialBalance;
        double accumulatedProfitRate = (double) (totalValue - initialBalance) / initialBalance * 100;

        return AccountResponse.of(username, totalValue, totalCash, snapshot.availableCash(),
                snapshot.reservedCash(), stockValue, buyValue, totalProfit, totalProfitRate, accumulatedProfit,
                accumulatedProfitRate, snapshot.stocks(), profitRates, profitAmounts, currentPrices,
                leverageResult.netValue(), leverageResult.loanTotal(), leverageResult.views());
    }

    /**
     * 레버리지 포지션 키("005930:X2")를 종목코드/배율로 분리해 화면용 뷰로 가공하고,
     * 순자산 합계(평가금액-대출금)와 대출금 합계를 함께 계산한다.
     */
    private LeverageCalcResult calculateLeverage(Map<String, LeveragePositionInfo> positions, Map<String, Integer> currentPrices) {

        List<LeveragePositionView> views = new ArrayList<>();
        long netValueTotal = 0L;
        long equityTotal = 0L;
        long loanTotal = 0L;
        long profitTotal = 0L;

        if (positions == null) {
            return new LeverageCalcResult(0L, 0L, 0L, 0L, views);
        }

        for (var entry : positions.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length != 2) {
                log.warn("Malformed leverage position key: {}", entry.getKey());
                continue;
            }
            String stockCode = parts[0];
            String leverageRatio = parts[1];
            LeveragePositionInfo info = entry.getValue();

            Integer curPrice = currentPrices.get(stockCode);
            if (curPrice == null) {
                BffServerStockSummaryTickMessage summary = stockSummaryRepository.findByStockCode(stockCode);
                if (summary == null) {
                    log.warn("No stock summary for leverage position stockCode={}", stockCode);
                    continue;
                }
                curPrice = summary.curPrice();
            }

            long evaluationAmount = (long) info.quantity() * curPrice;
            long netValue = evaluationAmount - info.loanAmount();
            long profitAmount = evaluationAmount - info.purchaseAmount();

            // 실제 내가 투입한 자기자본
            long equityAmount = info.purchaseAmount() - info.loanAmount();
            double profitRate = equityAmount == 0
                    ? 0
                    : profitAmount * 100.0 / equityAmount;

            views.add(new LeveragePositionView(stockCode, leverageRatio, info.quantity(), info.availableQuantity(),
                    info.purchaseAmount(), info.loanAmount(), evaluationAmount, netValue, profitRate, curPrice));

            netValueTotal += netValue;
            equityTotal += equityAmount;
            loanTotal += info.loanAmount();
            profitTotal += profitAmount;
        }

        return new LeverageCalcResult(netValueTotal, equityTotal, loanTotal, profitTotal, views);
    }
}
