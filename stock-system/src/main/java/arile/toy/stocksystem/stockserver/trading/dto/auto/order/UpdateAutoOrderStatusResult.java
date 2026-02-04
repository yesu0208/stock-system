package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import arile.toy.stocksystem.stockserver.trading.entity.AutoOrderEntity;

public record UpdateAutoOrderStatusResult(
        AutoOrderEntity autoOrderEntity,
        AutoOrderStatus previousStatus
) {
    public static UpdateAutoOrderStatusResult of(AutoOrderEntity autoOrderEntity, AutoOrderStatus previousStatus) {
        return new UpdateAutoOrderStatusResult(autoOrderEntity, previousStatus);
    }
}
