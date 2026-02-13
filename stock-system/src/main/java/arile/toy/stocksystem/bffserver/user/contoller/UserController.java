package arile.toy.stocksystem.bffserver.user.contoller;

import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.dto.UserLoginRequest;
import arile.toy.stocksystem.bffserver.user.dto.UserSignUpRequest;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> signUp(
            @Valid @RequestBody UserSignUpRequest userSignUpRequest
    ) {
        var userDto = userService.signUp(userSignUpRequest);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(
            @Valid @RequestBody UserLoginRequest userLoginRequest,
            HttpServletResponse response) {
        var userAuthenticationResponse = userService.authenticate(userLoginRequest, response);
        return ResponseEntity.ok(userAuthenticationResponse);
    }

//    @GetMapping
//    public ResponseEntity<String> getUser(
//            @AuthenticationPrincipal UserDetails user) {
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        return ResponseEntity.ok(user.getUsername());
//    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> userDtoList = userService.getAllUsers();
        return ResponseEntity.ok(userDtoList);
    }
}
