package arile.toy.stocksystem.stockserver.autoorder.repository;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutoOrderRepository extends JpaRepository<AutoOrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AutoOrderEntity o where o.autoOrderId = :autoOrderId")
    Optional<AutoOrderEntity> findByIdForUpdate(@Param("autoOrderId") Long autoOrderId);

    default List<AutoOrderEntity> findAllUntriggered(List<String> stockCodes) {
        List<AutoOrderStatus> openStatuses = Arrays.stream(AutoOrderStatus.values())
                .filter(AutoOrderStatus::isOpen)
                .toList();
        return findAllByAutoOrderStatusInAndStockCodeIn(openStatuses, stockCodes);
    }

    List<AutoOrderEntity> findAllByAutoOrderStatusInAndStockCodeIn(List<AutoOrderStatus> statuses, List<String> stockCodes);

    @Query("""
            select a from AutoOrderEntity a
            where a.username = :username
            and (:stockCode is null or a.stockCode = :stockCode)
            and (:statuses is null or a.autoOrderStatus in :statuses)
            and (:from is null or a.orderTime >= :from)
            and (:to is null or a.orderTime <= :to)
            order by a.orderTime desc
            """)
    Page<AutoOrderEntity> search(
            @Param("username") String username,
            @Param("stockCode") String stockCode,
            @Param("statuses") List<AutoOrderStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
