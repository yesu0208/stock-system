package arile.toy.stocksystem.accountserver.rank.repository;

import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRankRepository extends JpaRepository<UserRankEntity, Long> {
    Optional<UserRankEntity> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from UserRankEntity r where r.username = :username")
    Optional<UserRankEntity> findByUsernameForUpdate(@Param("username") String username);
}
