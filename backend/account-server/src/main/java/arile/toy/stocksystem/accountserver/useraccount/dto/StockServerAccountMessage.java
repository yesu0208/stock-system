package arile.toy.stocksystem.accountserver.useraccount.dto;

import java.util.Map;

public record StockServerAccountMessage(
        String username,
        Long availableCash,
        Long reservedCash,
        Map<String, StockInfo> stocks
) {
    public static StockServerAccountMessage of(
            String username, Long availableCash, Long reservedCash, Map<String, StockInfo> stocks) {
        return new StockServerAccountMessage(username, availableCash, reservedCash, stocks);
    }
}
