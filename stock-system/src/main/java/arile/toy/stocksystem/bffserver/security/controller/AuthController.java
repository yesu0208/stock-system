package arile.toy.stocksystem.bffserver.security.controller;

import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String username = jwtService.getUsernameFromRefreshToken(refreshToken);
            String jti = jwtService.getJtiFromRefreshToken(refreshToken);

            if (!refreshTokenRepository.exists(jti)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            refreshTokenRepository.delete(jti);

            var userDetails = userService.loadUserByUsername(username);

            String newAccessToken = jwtService.generateAccessToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            String newJti = jwtService.getJtiFromRefreshToken(newRefreshToken);

            refreshTokenRepository.save(
                    newJti,
                    username,
                    jwtService.getRefreshValidity()
            );

            Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) jwtService.getRefreshValidity() / 1000);
            response.addCookie(refreshCookie);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
