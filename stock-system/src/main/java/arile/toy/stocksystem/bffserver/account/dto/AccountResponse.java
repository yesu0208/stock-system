package arile.toy.stocksystem.bffserver.account.dto;

import java.util.Map;

public record AccountResponse(
        String username,
        Long totalValue,
        Long totalCash,
        Long availableCash,
        Long reservedCash,
        Long stockValue,
        Map<String, StockInfo> stocks,
        Map<String, Double> profitRates,
        Map<String, Long> profitAmounts
) {
    public static AccountResponse of(String username, Long totalValue, Long totalCash,
                                     Long availableCash, Long reservedCash, Long stockValue,
                                     Map<String, StockInfo> stocks, Map<String, Double> profitRates, Map<String, Long> profitAmounts) {
        return new AccountResponse(username, totalValue, totalCash, availableCash,
                reservedCash, stockValue, stocks, profitRates, profitAmounts);
    }
}
