package arile.toy.stocksystem.stockserver.trade.repository;

import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Long> {

    @Query("""
            select t from TradeEntity t
            where t.username = :username
            and (:stockCode is null or t.stockCode = :stockCode)
            and (:from is null or t.executedAt >= :from)
            and (:to is null or t.executedAt <= :to)
            order by t.executedAt desc
            """)
    Page<TradeEntity> search(
            @Param("username") String username,
            @Param("stockCode") String stockCode,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
