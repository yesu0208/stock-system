package arile.toy.stocksystem.bffserver.stocktalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockTalkSendRequest(
        @NotBlank
        @Size(max = 300)
        String content
) {
}
