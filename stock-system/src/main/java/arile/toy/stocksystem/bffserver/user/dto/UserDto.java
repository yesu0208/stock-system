package arile.toy.stocksystem.bffserver.user.dto;

import arile.toy.stocksystem.bffserver.user.entity.UserEntity;

public record UserDto(Long userId, String username) {
    public static UserDto fromEntity(UserEntity userEntity) {
        return new UserDto(userEntity.getUserId(), userEntity.getUsername());
    }
}
