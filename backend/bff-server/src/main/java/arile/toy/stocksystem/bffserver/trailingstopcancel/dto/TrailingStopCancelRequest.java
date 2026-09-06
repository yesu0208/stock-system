package arile.toy.stocksystem.bffserver.trailingstopcancel.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TrailingStopCancelRequest(
        @NotNull Long trailingStopId,
        @NotEmpty String stockCode
) {
}
