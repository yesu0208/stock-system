package arile.toy.stocksystem.bffserver.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeNicknameRequest(
        @NotEmpty
        @Size(min = 2, max = 10)
        @Pattern(regexp = "^[a-z0-9가-힣]+$", message = "닉네임은 영어 소문자, 한글, 숫자만 가능")
        String nickname
) {
}
