package arile.toy.stocksystem.bffserver.leverage.event;

public record LiquidationExecutedEvent(
        String username,
        String stockCode,
        String leverageRatio,
        Integer liquidatedQuantity,
        Long settlementPrice,
        Long shortfall
) {
}
