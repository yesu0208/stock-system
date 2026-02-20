package arile.toy.stocksystem.bffserver.user.dto;

import arile.toy.stocksystem.bffserver.user.entity.UserEntity;

import java.time.Instant;

public record UserDto(
        Long userId,
        String username,
        Instant createdDateTime)
{
    public static UserDto fromEntity(UserEntity userEntity) {
        return new UserDto(userEntity.getUserId(), userEntity.getUsername(),
                userEntity.getCreatedDateTime());
    }
}
