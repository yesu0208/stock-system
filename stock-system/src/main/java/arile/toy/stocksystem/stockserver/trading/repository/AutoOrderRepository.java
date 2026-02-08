package arile.toy.stocksystem.stockserver.trading.repository;

import arile.toy.stocksystem.stockserver.trading.entity.AutoOrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoOrderRepository extends JpaRepository<AutoOrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AutoOrderEntity o where o.autoOrderId = :autoOrderId")
    Optional<AutoOrderEntity> findByIdForUpdate(@Param("autoOrderId") Long autoOrderId);
}
