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

    /** account-server의 MarginRatioCalculator.MIN_MAINTENANCE_RATIO와 동일해야 한다.
     *  마이크로서비스 경계로 인해 직접 참조가 불가능해 값만 동일하게 복제해서 사용한다.
     *  이 값이 바뀌면 account-server 쪽도 함께 변경해야 한다. */
    private static final double MAINTENANCE_RATIO = 1.4; // 140%

    private static final double FEE_RATE = 0.00015;
    private static final double TAX_RATE = 0.0020;

    @Value("${account.initial-balance}")
    private long initialBalance;

    public AccountResponse calculate(String username) {
        AccountSnapshot snapshot = accountPullService.getAccountMessage(username);

        long totalCash = snapshot.availableCash() + snapshot.reservedCash();
        long stockValue = 0L;
        long buyValue = 0L;
        long costValueTotal = 0L;
        long stockProfitTotal = 0L;

        Map<String, Double> profitRates = new HashMap<>();
        Map<String, Long> profitAmounts = new HashMap<>();
        Map<String, Integer> currentPrices = new HashMap<>();

        for (var entry : snapshot.stocks().entrySet()) {
            String stockCode = entry.getKey();
            StockInfo stockInfo = entry.getValue();
            int quantity = stockInfo.quantity();
            long totalAmount = stockInfo.totalAmount();
            long totalCostAmount = stockInfo.totalCostAmount();

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

            // 평가손익 = 평가금액 - 매도비용(수수료+거래세) - 매입원금액
            long sellCost = Math.round(curStockValue * (FEE_RATE + TAX_RATE));
            long profitAmount = curStockValue - sellCost - totalCostAmount;
            // 수익률 = 평가손익 / 매입원금액 (현물은 대출금이 없어 매입원금액 = 투입원금액)
            double profitRate = totalCostAmount == 0 ? 0 : profitAmount * 100.0 / totalCostAmount;

            profitAmounts.put(stockCode, profitAmount);
            profitRates.put(stockCode, profitRate);

            costValueTotal += totalCostAmount;
            stockProfitTotal += profitAmount;
        }

        var leverageResult = calculateLeverage(snapshot.leveragePositions(), currentPrices);

        long totalValue = totalCash + stockValue + leverageResult.netValue();
        // 분모를 buyValue(순수 매입금액)가 아니라 costValueTotal(매입원금액)로 교체 — 개별 수익률 계산 기준과 일치시킴
        long totalEquityValue = costValueTotal + leverageResult.equityTotal();
        // stockValue - buyValue(수수료 미반영) 대신, 개별 profitAmount 합계(stockProfitTotal)를 그대로 합산
        long totalProfit = stockProfitTotal + leverageResult.profitTotal();
        double totalProfitRate = totalEquityValue != 0 ? totalProfit * 100.0 / totalEquityValue : 0;

        long accumulatedProfit = totalValue - initialBalance;
        double accumulatedProfitRate = (double) (totalValue - initialBalance) / initialBalance * 100;

        return AccountResponse.of(username, totalValue, totalCash, snapshot.availableCash(),
                snapshot.reservedCash(), stockValue, buyValue, totalProfit, totalProfitRate, accumulatedProfit,
                accumulatedProfitRate, snapshot.stocks(), profitRates, profitAmounts, currentPrices,
                leverageResult.netValue(), leverageResult.loanTotal(), leverageResult.views(),
                snapshot.marginStatus(), snapshot.accountStatus());
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

            // 평가손익 = 평가금액 - 매도비용(수수료+거래세) - 매입원금액(costAmount) — 현물과 동일 공식, 대출금은 상쇄되어 등장하지 않음
            long sellCost = Math.round(evaluationAmount * (FEE_RATE + TAX_RATE));
            long profitAmount = evaluationAmount - sellCost - info.costAmount();

            // 투입원금액 = 매입원금액 - 대출금 (실제 내가 낸 증거금+수수료)
            long investedAmount = info.costAmount() - info.loanAmount();
            double profitRate = investedAmount == 0
                    ? 0
                    : profitAmount * 100.0 / investedAmount;

            long initialMargin = info.purchaseAmount() - info.loanAmount(); // 순수 개시증거금(수수료 미포함) — 화면 표기용은 기존 그대로 유지
            long maintenanceMargin = Math.round(info.loanAmount() * MAINTENANCE_RATIO);
            long maintenancePrice  = info.quantity() > 0
                    ? Math.round((MAINTENANCE_RATIO * info.loanAmount()) / info.quantity())
                    : 0L;

            views.add(new LeveragePositionView(stockCode, leverageRatio, info.quantity(), info.availableQuantity(),
                    info.purchaseAmount(), info.costAmount(), info.loanAmount(), evaluationAmount, netValue, profitAmount, profitRate, curPrice,
                    info.marginStatus(), initialMargin, maintenanceMargin, maintenancePrice));

            netValueTotal += netValue;
            equityTotal += investedAmount; // equityTotal도 투입원금액 기준으로 통일 (기존엔 purchaseAmount-loanAmount였는데 이제 costAmount-loanAmount)
            loanTotal += info.loanAmount();
            profitTotal += profitAmount;
        }

        return new LeverageCalcResult(netValueTotal, equityTotal, loanTotal, profitTotal, views);
    }
}
