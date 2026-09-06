package arile.toy.stocksystem.bffserver.otococancel.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OtocoCancelRequest(
        @NotNull Long otocoId,
        @NotEmpty String stockCode
) {
}
