package arile.toy.stocksystem.domain.user;


import jakarta.validation.constraints.NotEmpty;

public record UserLoginRequestBody(
        @NotEmpty String username,
        @NotEmpty String password) {
}
