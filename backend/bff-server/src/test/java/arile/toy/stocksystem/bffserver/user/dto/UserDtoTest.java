package arile.toy.stocksystem.bffserver.user.dto;

import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoTest {

    private static UserEntity entity() {
        UserEntity entity = UserEntity.of("user1", "pw", "닉네임");
        entity.setUserId(1L);
        entity.setCreatedDateTime(Instant.parse("2026-09-01T00:00:00Z"));
        entity.changeProfileImageUrl("/uploads/profile/user1.png");
        return entity;
    }

    @Test
    @DisplayName("fromEntity: 비밀번호를 제외한 사용자 정보를 담고, 랭크는 비워 둔다")
    void fromEntity() {
        UserDto dto = UserDto.fromEntity(entity());

        assertThat(dto).isEqualTo(new UserDto(1L, "user1", "닉네임",
                Instant.parse("2026-09-01T00:00:00Z"), Role.USER, null, "/uploads/profile/user1.png"));
    }

    @Test
    @DisplayName("withRank: 랭크만 채운 새 DTO를 반환하고 원본은 바꾸지 않는다")
    void withRank() {
        UserDto original = UserDto.fromEntity(entity());
        RankResponse rank = new RankResponse("user1", "GOLD", 4, 3800L, "PLATINUM_5", 3750L, 4250L);

        UserDto withRank = original.withRank(rank);

        assertThat(withRank.rank()).isEqualTo(rank);
        assertThat(withRank.username()).isEqualTo("user1");
        assertThat(withRank.profileImageUrl()).isEqualTo("/uploads/profile/user1.png");
        assertThat(original.rank()).isNull();
    }
}
