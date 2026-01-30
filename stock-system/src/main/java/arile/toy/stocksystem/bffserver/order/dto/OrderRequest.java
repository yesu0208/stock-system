package arile.toy.stocksystem.bffserver.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotEmpty String stockCode,
        @NotNull OrderType orderType,
        @NotNull @Positive Integer orderPrice,
        @NotNull @Positive Integer orderQuantity
) {
}
