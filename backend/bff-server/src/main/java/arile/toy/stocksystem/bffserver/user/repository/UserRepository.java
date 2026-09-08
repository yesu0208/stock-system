package arile.toy.stocksystem.bffserver.user.repository;

import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByNickname(String nickname);

    List<UserProfileProjection> findByUsernameIn(Collection<String> usernames);

    interface UserProfileProjection {
        String getUsername();
        String getNickname();
        String getProfileImageUrl();
    }
}
