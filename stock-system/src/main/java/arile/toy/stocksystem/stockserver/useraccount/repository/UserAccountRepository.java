package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
