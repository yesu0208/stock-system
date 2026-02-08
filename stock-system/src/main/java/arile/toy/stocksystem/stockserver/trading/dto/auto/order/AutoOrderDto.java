package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import arile.toy.stocksystem.stockserver.trading.entity.AutoOrderEntity;

import java.time.Instant;

public record AutoOrderDto(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
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
                autoOrderEntity.getTriggerPrice(),
                autoOrderEntity.getOrderPrice(),
                autoOrderEntity.getOrderQuantity(),
                autoOrderEntity.getOrderTime());
    }
}
