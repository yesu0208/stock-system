package arile.toy.stocksystem.bffserver.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWatchListRequest(
        @NotBlank String stockCode,
        @NotBlank String stockName
) {
}
