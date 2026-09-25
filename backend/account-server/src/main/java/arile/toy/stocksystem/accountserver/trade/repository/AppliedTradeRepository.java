package arile.toy.stocksystem.accountserver.trade.repository;

import arile.toy.stocksystem.accountserver.trade.entity.AppliedTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppliedTradeRepository extends JpaRepository<AppliedTradeEntity, Long> {

    boolean existsByStockCodeAndTradeId(String stockCode, Long tradeId);
}
