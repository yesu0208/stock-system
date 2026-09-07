package arile.toy.stocksystem.stockserver.alert.repository;

import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AlertEntity a where a.alertId = :alertId")
    Optional<AlertEntity> findByIdForUpdate(@Param("alertId") Long alertId);

    default List<AlertEntity> findAllActive(List<String> stockCodes) {
        List<AlertStatus> openStatuses = Arrays.stream(AlertStatus.values())
                .filter(AlertStatus::isOpen)
                .toList();
        return findAllByStatusInAndStockCodeIn(openStatuses, stockCodes);
    }

    List<AlertEntity> findAllByStatusInAndStockCodeIn(List<AlertStatus> statuses, List<String> stockCodes);
}
