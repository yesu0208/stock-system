package arile.toy.stocksystem.bffserver.account.dto;

import java.util.List;
import java.util.Map;

public record AccountResponse(
        String username,
        Long totalValue,
        Long totalCash,
        Long availableCash,
        Long reservedCash,
        Long stockValue,
        Long buyValue,
        Long totalProfit,
        Double totalProfitRate,
        Long accumulatedProfit,
        Double accumulatedProfitRate,
        Map<String, StockInfo> stocks,
        Map<String, Double> profitRates,
        Map<String, Long> profitAmounts,
        Map<String, Integer> currentPrices,
        Long leverageNetValue,
        Long leverageLoanTotal,
        List<LeveragePositionView> leveragePositions
) {
    public static AccountResponse of(String username, Long totalValue, Long totalCash,
                                     Long availableCash, Long reservedCash, Long stockValue, Long buyValue, Long totalProfit, Double totalProfitRate,
                                     Long accumulatedProfit, Double accumulatedProfitRate, Map<String, StockInfo> stocks, Map<String, Double> profitRates, Map<String, Long> profitAmounts,
                                     Map<String, Integer> currentPrices, Long leverageNetValue, Long leverageLoanTotal, List<LeveragePositionView> leveragePositions) {
        return new AccountResponse(username, totalValue, totalCash, availableCash,
                reservedCash, stockValue, buyValue, totalProfit, totalProfitRate, accumulatedProfit, accumulatedProfitRate,
                stocks, profitRates, profitAmounts, currentPrices, leverageNetValue, leverageLoanTotal, leveragePositions);
    }
}
