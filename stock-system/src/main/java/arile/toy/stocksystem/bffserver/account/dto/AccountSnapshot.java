package arile.toy.stocksystem.bffserver.account.dto;

import java.util.Map;

public record AccountSnapshot(
        Long availableCash,
        Long reservedCash,
        Map<String, StockInfo> stocks
) {
    public static AccountSnapshot of(Long availableCash, Long reservedCash, Map<String, StockInfo> stocks) {
        return new AccountSnapshot(availableCash, reservedCash, stocks);
    }
}
