package arile.toy.stocksystem.bffserver.user.dto;


import jakarta.validation.constraints.NotEmpty;

public record UserLoginRequest(
        @NotEmpty String username,
        @NotEmpty String password) {
}
