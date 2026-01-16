package arile.toy.stocksystem.domain.user;

import arile.toy.stocksystem.domain.entity.UserEntity;

public record UserDto(Long userId, String username) {
    public static UserDto fromEntity(UserEntity userEntity) {
        return new UserDto(userEntity.getUserId(), userEntity.getUsername());
    }
}
