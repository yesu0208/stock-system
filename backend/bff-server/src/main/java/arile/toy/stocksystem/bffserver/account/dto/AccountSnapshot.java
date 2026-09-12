package arile.toy.stocksystem.bffserver.account.dto;

import java.util.Map;

public record AccountSnapshot(
        Long availableCash,
        Long reservedCash,
        Map<String, StockInfo> stocks,
        Map<String, LeveragePositionInfo> leveragePositions,
        String marginStatus
) {
    public static AccountSnapshot of(Long availableCash, Long reservedCash, Map<String, StockInfo> stocks,
                                     Map<String, LeveragePositionInfo> leveragePositions, String marginStatus) {
        return new AccountSnapshot(availableCash, reservedCash, stocks, leveragePositions, marginStatus);
    }
}
