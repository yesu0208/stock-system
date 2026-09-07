package arile.toy.stocksystem.bffserver.alert.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlertRequest(
        @NotEmpty String stockCode,
        @NotNull AlertDirection direction,
        @NotNull @Positive Integer triggerPrice
) {
}
