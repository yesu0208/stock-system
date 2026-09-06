package arile.toy.stocksystem.bffserver.trailingstop.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TrailingStopRequest(
        @NotEmpty String stockCode,
        @NotNull TrailingStopType trailingStopType,
        @NotNull @Positive Integer orderQuantity,
        @NotNull @DecimalMin("0.1") Double stopPercent,
        @NotNull @Positive Integer basePrice,
        LeverageRatio leverageRatio
) {
    public LeverageRatio leverageRatioOrDefault() {
        return leverageRatio == null ? LeverageRatio.SPOT : leverageRatio;
    }
}
