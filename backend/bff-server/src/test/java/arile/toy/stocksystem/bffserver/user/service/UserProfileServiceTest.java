package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.user.dto.UserProfile;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService service;

    private static UserRepository.UserProfileProjection projection(String username, String nickname, String imageUrl) {
        UserRepository.UserProfileProjection p = mock(UserRepository.UserProfileProjection.class);
        given(p.getUsername()).willReturn(username);
        given(p.getNickname()).willReturn(nickname);
        given(p.getProfileImageUrl()).willReturn(imageUrl);
        return p;
    }

    @Test
    @DisplayName("단건: 사용자의 아이디·닉네임·프로필 이미지를 프로필로 만든다")
    void getProfile() {
        UserEntity entity = mock(UserEntity.class);
        given(entity.getUsername()).willReturn("user1");
        given(entity.getNickname()).willReturn("개미");
        given(entity.getProfileImageUrl()).willReturn("/uploads/profile/user1.png");
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(entity));

        assertThat(service.getProfile("user1"))
                .isEqualTo(new UserProfile("user1", "개미", "/uploads/profile/user1.png"));
    }

    @Test
    @DisplayName("단건: 탈퇴 등으로 사용자가 없으면 아이디를 닉네임으로 쓰는 빈 프로필을 반환한다")
    void getProfile_missing() {
        given(userRepository.findByUsername("gone")).willReturn(Optional.empty());

        assertThat(service.getProfile("gone")).isEqualTo(new UserProfile("gone", "gone", null));
    }

    @Test
    @DisplayName("여러 명: 중복을 제거해 한 번에 조회하고, 아이디별 프로필 맵으로 반환한다")
    void getProfiles() {
        // 목 행은 given(...) 밖에서 먼저 만든다 (given 괄호 안에서 다른 목을 설정하면 UnfinishedStubbingException)
        List<UserRepository.UserProfileProjection> rows = List.of(
                projection("user1", "개미", "/uploads/profile/user1.png"),
                projection("user2", "황소", null));
        given(userRepository.findByUsernameIn(Set.of("user1", "user2"))).willReturn(rows);

        Map<String, UserProfile> profiles = service.getProfiles(List.of("user1", "user2", "user1"));

        assertThat(profiles).containsOnly(
                Map.entry("user1", new UserProfile("user1", "개미", "/uploads/profile/user1.png")),
                Map.entry("user2", new UserProfile("user2", "황소", null)));
        verify(userRepository).findByUsernameIn(Set.of("user1", "user2"));
    }

    @Test
    @DisplayName("여러 명: 없는 사용자는 맵에 넣지 않는다 (호출하는 쪽이 빈 프로필로 채움)")
    void getProfiles_missingUserOmitted() {
        List<UserRepository.UserProfileProjection> rows = List.of(projection("user1", "개미", null));
        given(userRepository.findByUsernameIn(Set.of("user1", "gone"))).willReturn(rows);

        Map<String, UserProfile> profiles = service.getProfiles(List.of("user1", "gone"));

        assertThat(profiles).containsOnlyKeys("user1");
    }

    @Test
    @DisplayName("여러 명: 목록이 비어 있으면 DB를 조회하지 않고 빈 맵을 반환한다")
    void getProfiles_empty() {
        assertThat(service.getProfiles(List.of())).isEmpty();

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("빈 프로필: 아이디를 닉네임 자리에 쓰고 이미지는 없다")
    void emptyProfile() {
        assertThat(UserProfile.empty("gone")).isEqualTo(new UserProfile("gone", "gone", null));
    }
}
