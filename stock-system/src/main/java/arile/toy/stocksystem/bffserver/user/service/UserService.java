package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.dto.UserLoginRequest;
import arile.toy.stocksystem.bffserver.user.dto.UserSignUpRequest;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired private JwtService jwtService;

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

        return UserDto.fromEntity(userEntity);
    }

    public UserAuthenticationResponse authenticate(UserLoginRequest userLoginRequest) {
        var userEntity = getUserEntityByUsername(userLoginRequest.username());

        if (bCryptPasswordEncoder.matches(userLoginRequest.password(), userEntity.getPassword())) {
            var accessToken = jwtService.generateAccessToken(userEntity);
            return new UserAuthenticationResponse(accessToken);
        } else {
            throw new UserNotFoundException();
        }
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
