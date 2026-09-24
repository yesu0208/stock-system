package arile.toy.stocksystem.bffserver.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeEditRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 4000) String content
) {
}
