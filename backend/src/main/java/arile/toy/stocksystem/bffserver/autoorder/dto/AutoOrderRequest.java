package arile.toy.stocksystem.bffserver.autoorder.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AutoOrderRequest(
        @NotEmpty String stockCode,
        @NotNull AutoOrderType autoOrderType,
        @NotNull @Positive Integer triggerPrice,
        @NotNull @Positive Integer orderPrice,
        @NotNull @Positive Integer orderQuantity
) {
}
