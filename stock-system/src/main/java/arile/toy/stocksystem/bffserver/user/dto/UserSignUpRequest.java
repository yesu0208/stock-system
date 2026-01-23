package arile.toy.stocksystem.bffserver.user.dto;


import jakarta.validation.constraints.NotEmpty;

public record UserSignUpRequest(
        @NotEmpty String username,
        @NotEmpty String password) {
}
