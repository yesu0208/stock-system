package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderDto(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime
) {
    public static AutoOrderDto fromEntity(AutoOrderEntity autoOrderEntity) {
        return new AutoOrderDto(autoOrderEntity.getAutoOrderId(),
                autoOrderEntity.getUsername(),
                autoOrderEntity.getStockCode(),
                autoOrderEntity.getAutoOrderType(),
                autoOrderEntity.getLeverageRatio(),
                autoOrderEntity.getTriggerPrice(),
                autoOrderEntity.getOrderPrice(),
                autoOrderEntity.getOrderQuantity(),
                autoOrderEntity.getOrderTime());
    }
}
