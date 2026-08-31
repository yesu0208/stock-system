package arile.toy.stocksystem.bffserver.discussion.dto;

import jakarta.validation.constraints.NotBlank;

public record PostEditRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
