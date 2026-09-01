package arile.toy.stocksystem.accountserver.leverage.event;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;

public record MarginCallEvent(
        String username,
        String stockCode,
        LeverageRatio leverageRatio,
        MarginStatus status,
        Double marginRatio
) {
    public static MarginCallEvent of(String username, String stockCode, LeverageRatio leverageRatio,
                                     MarginStatus status, Double marginRatio) {
        return new MarginCallEvent(username, stockCode, leverageRatio, status, marginRatio);
    }
}
