package arile.toy.stocksystem.bffserver.user.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSignUpRequest(
        @NotEmpty
        @Size(min = 4, max = 20)
        @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 소문자, 숫자만 가능")
        String username,

        @NotEmpty
        @Size(min = 2, max = 10)
        @Pattern(regexp = "^[a-z0-9가-힣]+$", message = "닉네임은 영어 소문자, 한글, 숫자만 가능")
        String nickname,

        @NotEmpty
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*]).+$",
                message = "비밀번호는 소문자, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다"
        )
        String password
) {
}
