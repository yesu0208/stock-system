package arile.toy.stocksystem.controller;

import arile.toy.stocksystem.domain.user.UserAuthenticationResponse;
import arile.toy.stocksystem.domain.user.UserDto;
import arile.toy.stocksystem.domain.user.UserLoginRequestBody;
import arile.toy.stocksystem.domain.user.UserSignUpRequestBody;
import arile.toy.stocksystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> signUp(
            @Valid @RequestBody UserSignUpRequestBody userSignUpRequestBody
    ) {
        var userDto = userService.signUp(userSignUpRequestBody);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(
            @Valid @RequestBody UserLoginRequestBody userLoginRequestBody) {
        var response = userService.authenticate(userLoginRequestBody);
        return ResponseEntity.ok(response);
    }
}
