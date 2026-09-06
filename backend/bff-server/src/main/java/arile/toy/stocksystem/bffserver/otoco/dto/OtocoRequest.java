package arile.toy.stocksystem.bffserver.otoco.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OtocoRequest(
        @NotEmpty String stockCode,
        @NotNull OtocoEntryDirection entryDirection,
        @NotNull @Positive Integer orderQuantity,
        @NotNull @Positive Integer entryTriggerPrice,
        @NotNull OtocoExitMode tpMode,
        Integer tpPrice,
        Double tpPct,
        @NotNull OtocoExitMode slMode,
        Integer slPrice,
        Double slPct,
        LeverageRatio leverageRatio
) {
    public LeverageRatio leverageRatioOrDefault() {
        return leverageRatio == null ? LeverageRatio.SPOT : leverageRatio;
    }
}
