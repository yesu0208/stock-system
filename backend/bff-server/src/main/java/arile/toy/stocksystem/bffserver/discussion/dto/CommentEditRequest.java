package arile.toy.stocksystem.bffserver.discussion.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentEditRequest(
        @NotBlank String content
) {
}
