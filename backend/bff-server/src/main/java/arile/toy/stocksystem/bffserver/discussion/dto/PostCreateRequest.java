package arile.toy.stocksystem.bffserver.discussion.dto;

import jakarta.validation.constraints.NotBlank;

public record PostCreateRequest(
        @NotBlank String stockCode,
        @NotBlank String stockName,
        @NotBlank String title,
        @NotBlank String content
) {
}
