package arile.toy.stocksystem.stockserver.trade.repository;

import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
}
