package arile.toy.stocksystem.bffserver.cancel.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CancelRequest(
        @NotNull Long orderId,
        @NotEmpty String stockCode
) {
}
