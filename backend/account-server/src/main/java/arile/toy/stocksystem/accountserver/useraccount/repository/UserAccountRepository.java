package arile.toy.stocksystem.accountserver.useraccount.repository;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsername(String username);
    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAccountEntity u WHERE u.username = :username")
    Optional<UserAccountEntity> findByUsernameForUpdate(@Param("username") String username);

    @Query("SELECT u.username FROM UserAccountEntity u")
    List<String> findAllUsernames();

    List<UserAccountEntity> findByAccountStatus(AccountStatus accountStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAccountEntity u WHERE u.userAccountId = :id")
    Optional<UserAccountEntity> findByIdForUpdate(@Param("id") Long id);
}
