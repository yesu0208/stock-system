package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException;
import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.dto.UserLoginRequest;
import arile.toy.stocksystem.bffserver.user.dto.UserSignUpRequest;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import arile.toy.stocksystem.bffserver.user.event.publisher.UserCreatedEventPublisher;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final UserCreatedEventPublisher userCreatedEventPublisher;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UserNotFoundException {
        return getUserEntityByUsername(username);
    }

    public UserDto signUp(UserSignUpRequest userSignUpRequest) {
        userRepository.findByUsername(userSignUpRequest.username())
                .ifPresent(userEntity -> {
                    throw new UserAlreadyExistsException();
                });

        var userEntity = userRepository.save(
                UserEntity.of(
                        userSignUpRequest.username(),
                        bCryptPasswordEncoder.encode(userSignUpRequest.password())
                )
        );

        userCreatedEventPublisher.publishUserCreatedEvent(UserCreatedEvent.of(userEntity.getUsername()));

        return UserDto.fromEntity(userEntity);
    }

    public UserAuthenticationResponse authenticate(UserLoginRequest userLoginRequest, HttpServletResponse response) {
        var userEntity = getUserEntityByUsername(userLoginRequest.username());

        if (!bCryptPasswordEncoder.matches(userLoginRequest.password(), userEntity.getPassword())) {
            throw new UserNotFoundException();
        }

        var accessToken = jwtService.generateAccessToken(userEntity);
        var refreshToken = jwtService.generateRefreshToken(userEntity);

        String jti = jwtService.getJtiFromRefreshToken(refreshToken);

        refreshTokenRepository.save(jti, userEntity.getUsername(),
                jwtService.getRefreshValidity());

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) jwtService.getRefreshValidity()/1000);
        response.addCookie(refreshCookie);

        return new UserAuthenticationResponse(accessToken);
    }

    private UserEntity getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDto::fromEntity)
                .toList();
    }
}
