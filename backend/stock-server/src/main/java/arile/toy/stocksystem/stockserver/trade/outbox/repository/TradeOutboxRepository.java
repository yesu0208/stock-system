package arile.toy.stocksystem.stockserver.trade.outbox.repository;

import arile.toy.stocksystem.stockserver.trade.outbox.entity.OutboxStatus;
import arile.toy.stocksystem.stockserver.trade.outbox.entity.TradeOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeOutboxRepository extends JpaRepository<TradeOutboxEntity, Long> {
    List<TradeOutboxEntity> findTop100ByStatusOrderByOutboxIdAsc(OutboxStatus status);
}
