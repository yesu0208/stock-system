package arile.toy.stocksystem.bffserver.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotEmpty String stockCode,
        @NotNull OrderType orderType,
        @NotNull @Positive Integer orderPrice,
        @NotNull @Positive Integer orderQuantity,
        LeverageRatio leverageRatio
) {
    /** leverageRatio가 null이면 spot으로 간주 */
    public LeverageRatio leverageRatioOrDefault() {
        return leverageRatio == null ? LeverageRatio.SPOT : leverageRatio;
    }
}
