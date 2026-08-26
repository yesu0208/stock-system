package arile.toy.stocksystem.bffserver.user.contoller;

import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.*;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null) {
            try {
                String jti = jwtService.getJtiFromRefreshToken(refreshToken);
                refreshTokenRepository.delete(jti);
            } catch (Exception ignored) {
            }
        }

        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setHttpOnly(true);
        deleteCookie.setSecure(false);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        response.addCookie(deleteCookie);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean exists = userService.isUsernameExists(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        boolean exists = userService.isNicknameExists(nickname);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> userDtoList = userService.getAllUsers();
        return ResponseEntity.ok(userDtoList);
    }

    @GetMapping("/user")
    public ResponseEntity<UserDto> getUser(
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = user.getUsername();

        UserDto userDto = userService.getUserByUsername(username);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping("/password")
    public ResponseEntity<UserDto> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDto userDto = userService.changePassword(user.getUsername(), changePasswordRequest);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<UserDto> changeNickname(
            @Valid @RequestBody ChangeNicknameRequest changeNicknameRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDto userDto = userService.changeNickname(user.getUsername(), changeNicknameRequest);
        return ResponseEntity.ok(userDto);
    }
}
