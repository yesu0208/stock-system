package arile.toy.stocksystem.stockserver.userstock.repository;

import arile.toy.stocksystem.stockserver.userstock.entity.UserStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStockRepository extends JpaRepository<UserStockEntity, Long> {
    Optional<UserStockEntity> findByUsernameAndStockCode(String username, String stockCode);
    List<UserStockEntity> findByUsername(String username);
}
