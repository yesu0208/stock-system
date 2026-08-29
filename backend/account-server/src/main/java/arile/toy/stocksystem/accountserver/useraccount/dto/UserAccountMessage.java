package arile.toy.stocksystem.accountserver.useraccount.dto;

import java.util.Map;

public record UserAccountMessage(
        String username,
        Long availableCash,
        Long reservedCash,
        Map<String, StockInfo> stocks
) {
    public static UserAccountMessage of(
            String username, Long availableCash, Long reservedCash, Map<String, StockInfo> stocks) {
        return new UserAccountMessage(username, availableCash, reservedCash, stocks);
    }
}
