package arile.toy.stocksystem.accountserver.leverage.repository;

import arile.toy.stocksystem.accountserver.leverage.entity.LeverageLiquidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeverageLiquidationRepository extends JpaRepository<LeverageLiquidationEntity, Long> {
}
