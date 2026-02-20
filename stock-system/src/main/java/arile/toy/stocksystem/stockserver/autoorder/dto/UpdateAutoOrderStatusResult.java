package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;

public record UpdateAutoOrderStatusResult(
        AutoOrderEntity autoOrderEntity,
        AutoOrderStatus previousStatus
) {
    public static UpdateAutoOrderStatusResult of(AutoOrderEntity autoOrderEntity, AutoOrderStatus previousStatus) {
        return new UpdateAutoOrderStatusResult(autoOrderEntity, previousStatus);
    }
}
