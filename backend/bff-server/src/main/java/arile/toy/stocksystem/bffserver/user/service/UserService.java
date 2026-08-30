package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.exception.user.NicknameAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.PasswordMismatchException;
import arile.toy.stocksystem.bffserver.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException;
import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.*;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import arile.toy.stocksystem.bffserver.user.event.publisher.UserCreatedEventPublisher;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import arile.toy.stocksystem.bffserver.user.storage.ProfileImageStorage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final UserCreatedEventPublisher userCreatedEventPublisher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProfileImageStorage profileImageStorage;

    @Override
    public UserDetails loadUserByUsername(String username) throws UserNotFoundException {
        return getUserEntityByUsername(username);
    }

    public UserDto getUserByUsername(String username) {
        var userEntity = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        return UserDto.fromEntity(userEntity);
    }

    public UserDto signUp(UserSignUpRequest userSignUpRequest) {
        userRepository.findByUsername(userSignUpRequest.username())
                .ifPresent(userEntity -> {
                    throw new UserAlreadyExistsException();
                });

        if (userRepository.existsByNickname(userSignUpRequest.nickname())) {
            throw new NicknameAlreadyExistsException();
        }

        var userEntity = userRepository.save(
                UserEntity.of(
                        userSignUpRequest.username(),
                        bCryptPasswordEncoder.encode(userSignUpRequest.password()),
                        userSignUpRequest.nickname()
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

    @Transactional
    public UserDto changePassword(String username, ChangePasswordRequest request) {
        var userEntity = getUserEntityByUsername(username);

        if (!bCryptPasswordEncoder.matches(request.currentPassword(), userEntity.getPassword())) {
            throw new PasswordMismatchException();
        }

        userEntity.changePassword(bCryptPasswordEncoder.encode(request.newPassword()));

        return UserDto.fromEntity(userEntity);
    }

    @Transactional
    public UserDto changeNickname(String username, ChangeNicknameRequest request) {
        var userEntity = getUserEntityByUsername(username);

        if (!userEntity.getNickname().equals(request.nickname())
                && userRepository.existsByNickname(request.nickname())) {
            throw new NicknameAlreadyExistsException();
        }

        userEntity.changeNickname(request.nickname());

        return UserDto.fromEntity(userEntity);
    }

    @Transactional
    public UserDto changeProfileImage(String username, MultipartFile file) {
        var userEntity = getUserEntityByUsername(username);

        String imageUrl = profileImageStorage.store(file, username);
        userEntity.changeProfileImageUrl(imageUrl);

        return UserDto.fromEntity(userEntity);
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

    public boolean isUsernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean isNicknameExists(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}