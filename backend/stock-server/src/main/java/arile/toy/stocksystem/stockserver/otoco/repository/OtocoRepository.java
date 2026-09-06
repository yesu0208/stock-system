package arile.toy.stocksystem.stockserver.otoco.repository;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
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
public interface OtocoRepository extends JpaRepository<OtocoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtocoEntity o where o.otocoId = :otocoId")
    Optional<OtocoEntity> findByIdForUpdate(@Param("otocoId") Long otocoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtocoEntity o where o.entryOrderId = :entryOrderId")
    Optional<OtocoEntity> findByEntryOrderIdForUpdate(@Param("entryOrderId") Long entryOrderId);

    default List<OtocoEntity> findAllUnfinished(List<String> stockCodes) {
        List<OtocoStatus> openStatuses = Arrays.stream(OtocoStatus.values())
                .filter(OtocoStatus::isOpen)
                .toList();
        return findAllByOtocoStatusInAndStockCodeIn(openStatuses, stockCodes);
    }

    List<OtocoEntity> findAllByOtocoStatusInAndStockCodeIn(List<OtocoStatus> statuses, List<String> stockCodes);
}
