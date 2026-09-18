package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotEmpty String stockCode,
        @NotNull OrderType orderType,
        @NotNull @Positive Integer orderPrice,
        @NotNull @Positive Integer orderQuantity,
        LeverageRatio leverageRatio,
        OrderExecutionType orderExecutionType
) {
    /** leverageRatio가 null이면 spot으로 간주 */
    public LeverageRatio leverageRatioOrDefault() {
        return leverageRatio == null ? LeverageRatio.SPOT : leverageRatio;
    }

    /** orderExecutionType이 null이면 기존 클라이언트 호환을 위해 LIMIT으로 간주 */
    public OrderExecutionType orderExecutionTypeOrDefault() {
        return orderExecutionType == null ? OrderExecutionType.LIMIT : orderExecutionType;
    }
}
