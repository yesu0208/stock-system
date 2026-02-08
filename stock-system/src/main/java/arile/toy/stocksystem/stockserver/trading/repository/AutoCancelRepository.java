package arile.toy.stocksystem.stockserver.trading.repository;

import arile.toy.stocksystem.stockserver.trading.entity.AutoCancelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoCancelRepository extends JpaRepository<AutoCancelEntity, Long> {
}
