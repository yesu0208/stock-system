package arile.toy.stocksystem.bffserver.alertcancel.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AlertCancelRequest(
        @NotNull Long alertId,
        @NotEmpty String stockCode
) {
}
