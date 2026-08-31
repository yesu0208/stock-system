package arile.toy.stocksystem.bffserver.user.dto;

import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;

import java.time.Instant;

public record UserDto(
        Long userId,
        String username,
        String nickname,
        Instant createdDateTime,
        RankResponse rank
) {
    public static UserDto fromEntity(UserEntity userEntity) {
        return new UserDto(userEntity.getUserId(), userEntity.getUsername(), userEntity.getNickname(),
                userEntity.getCreatedDateTime(), null);
    }

    public UserDto withRank(RankResponse rank) {
        return new UserDto(this.userId, this.username, this.nickname, this.createdDateTime, rank);
    }
}
