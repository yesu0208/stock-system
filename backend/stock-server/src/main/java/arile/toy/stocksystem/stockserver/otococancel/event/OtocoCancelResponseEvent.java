package arile.toy.stocksystem.stockserver.otococancel.event;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otococancel.dto.OtocoCancelErrorCode;

public record OtocoCancelResponseEvent(
        Long otocoId,
        String username,
        String stockCode,
        OtocoEntryDirection entryDirection,
        Integer entryTriggerPrice,
        Integer orderQuantity,
        boolean success,
        OtocoCancelErrorCode errorCode
) {
    public static OtocoCancelResponseEvent of(OtocoEntity entity, boolean success, OtocoCancelErrorCode errorCode) {
        return new OtocoCancelResponseEvent(
                entity.getOtocoId(), entity.getUsername(), entity.getStockCode(), entity.getEntryDirection(),
                entity.getEntryTriggerPrice(), entity.getOrderQuantity(), success, errorCode
        );
    }
}
