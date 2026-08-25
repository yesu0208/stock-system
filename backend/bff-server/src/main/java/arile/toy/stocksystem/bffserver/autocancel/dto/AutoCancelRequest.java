package arile.toy.stocksystem.bffserver.autocancel.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AutoCancelRequest(
        @NotNull Long autoOrderId,
        @NotEmpty String stockCode
) {
}
