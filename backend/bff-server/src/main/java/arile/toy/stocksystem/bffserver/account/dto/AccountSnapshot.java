package arile.toy.stocksystem.bffserver.account.dto;

import java.util.Map;

public record AccountSnapshot(
        Long availableCash,
        Long reservedCash,
        Map<String, StockInfo> stocks,
        Map<String, LeveragePositionInfo> leveragePositions
) {
    public static AccountSnapshot of(Long availableCash, Long reservedCash,
                                     Map<String, StockInfo> stocks, Map<String, LeveragePositionInfo> leveragePositions) {
        return new AccountSnapshot(availableCash, reservedCash, stocks, leveragePositions);
    }
}
