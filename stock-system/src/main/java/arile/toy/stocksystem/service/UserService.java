package arile.toy.stocksystem.service;

import arile.toy.stocksystem.domain.entity.UserEntity;
import arile.toy.stocksystem.domain.user.UserAuthenticationResponse;
import arile.toy.stocksystem.domain.user.UserDto;
import arile.toy.stocksystem.domain.user.UserLoginRequestBody;
import arile.toy.stocksystem.domain.user.UserSignUpRequestBody;
import arile.toy.stocksystem.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.exception.user.UserNotFoundException;
import arile.toy.stocksystem.repository.UserEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    @Autowired private UserEntityRepository userEntityRepository;
    @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired private JwtService jwtService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UserNotFoundException {
        return getUserEntityByUsername(username);
    }

    public UserDto signUp(UserSignUpRequestBody userSignUpRequestBody) {
        userEntityRepository.findByUsername(userSignUpRequestBody.username())
                .ifPresent(userEntity -> {
                    throw new UserAlreadyExistsException();
                });

        var userEntity = userEntityRepository.save(
                UserEntity.of(
                        userSignUpRequestBody.username(),
                        bCryptPasswordEncoder.encode(userSignUpRequestBody.password())
                )
        );

        return UserDto.from(userEntity);
    }

    public UserAuthenticationResponse authenticate(UserLoginRequestBody userLoginRequestBody) {
        var userEntity = getUserEntityByUsername(userLoginRequestBody.username());

        if (bCryptPasswordEncoder.matches(userLoginRequestBody.password(), userEntity.getPassword())) {
            var accessToken = jwtService.generateAccessToken(userEntity);
            return new UserAuthenticationResponse(accessToken);
        } else {
            throw new UserNotFoundException();
        }
    }

    private UserEntity getUserEntityByUsername(String username) {
        return userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
