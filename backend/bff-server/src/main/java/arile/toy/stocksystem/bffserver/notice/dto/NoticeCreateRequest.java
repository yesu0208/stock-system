package arile.toy.stocksystem.bffserver.notice.dto;

import jakarta.validation.constraints.NotBlank;

public record NoticeCreateRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
