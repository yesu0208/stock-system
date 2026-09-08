package arile.toy.stocksystem.stockserver.trailingstop.repository;

import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
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
public interface TrailingStopRepository extends JpaRepository<TrailingStopEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TrailingStopEntity t where t.trailingStopId = :trailingStopId")
    Optional<TrailingStopEntity> findByIdForUpdate(@Param("trailingStopId") Long trailingStopId);

    default List<TrailingStopEntity> findAllUntriggered(List<String> stockCodes) {
        List<TrailingStopStatus> openStatuses = Arrays.stream(TrailingStopStatus.values())
                .filter(TrailingStopStatus::isOpen)
                .toList();
        return findAllByTrailingStopStatusInAndStockCodeIn(openStatuses, stockCodes);
    }

    List<TrailingStopEntity> findAllByTrailingStopStatusInAndStockCodeIn(List<TrailingStopStatus> statuses, List<String> stockCodes);

    @Query("""
            select t from TrailingStopEntity t
            where t.username = :username
            and (:stockCode is null or t.stockCode = :stockCode)
            and (:statuses is null or t.trailingStopStatus in :statuses)
            and (:from is null or t.orderTime >= :from)
            and (:to is null or t.orderTime <= :to)
            order by t.orderTime desc
            """)
    Page<TrailingStopEntity> search(
            @Param("username") String username,
            @Param("stockCode") String stockCode,
            @Param("statuses") List<TrailingStopStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
