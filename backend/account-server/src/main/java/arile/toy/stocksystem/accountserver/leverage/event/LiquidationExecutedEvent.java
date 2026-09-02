package arile.toy.stocksystem.accountserver.leverage.event;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;

public record LiquidationExecutedEvent(
        String username,
        String stockCode,
        LeverageRatio leverageRatio,
        Integer liquidatedQuantity,
        Long settlementPrice,
        Long shortfall
) {
    public static LiquidationExecutedEvent of(String username, String stockCode, LeverageRatio leverageRatio,
                                              Integer liquidatedQuantity, Long settlementPrice, Long shortfall) {
        return new LiquidationExecutedEvent(username, stockCode, leverageRatio, liquidatedQuantity, settlementPrice, shortfall);
    }
}
