package arile.toy.stocksystem.bffserver.stocktalk.dto;

import jakarta.validation.constraints.NotBlank;

public record StockTalkSendRequest(
        @NotBlank String content
) {
}
