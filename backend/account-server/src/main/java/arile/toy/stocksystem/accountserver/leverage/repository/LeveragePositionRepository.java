package arile.toy.stocksystem.accountserver.leverage.repository;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeveragePositionRepository extends JpaRepository<LeveragePositionEntity, Long> {

    Optional<LeveragePositionEntity> findByUsernameAndStockCodeAndLeverageRatio(
            String username, String stockCode, arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio leverageRatio);

    List<LeveragePositionEntity> findByUsername(String username);

    /** 마진콜/이자 배치가 전체를 순회할 때 사용 */
    List<LeveragePositionEntity> findAll();

    List<LeveragePositionEntity> findByMarginStatus(MarginStatus marginStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LeveragePositionEntity p where p.leveragePositionId = :id")
    Optional<LeveragePositionEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LeveragePositionEntity p where p.username = :username and p.stockCode = :stockCode and p.leverageRatio = :leverageRatio")
    Optional<LeveragePositionEntity> findByUsernameAndStockCodeAndLeverageRatioForUpdate(
            @Param("username") String username,
            @Param("stockCode") String stockCode,
            @Param("leverageRatio") arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio leverageRatio);
}
