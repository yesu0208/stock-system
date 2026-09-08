package arile.toy.stocksystem.accountserver.dailyreturn.repository;

import arile.toy.stocksystem.accountserver.dailyreturn.entity.DailyReturnHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailyReturnHistoryRepository extends JpaRepository<DailyReturnHistoryEntity, Long> {

    boolean existsByUsernameAndRecordDate(String username, LocalDate recordDate);

    @Query("""
            select d from DailyReturnHistoryEntity d
            where d.username = :username
            and (:from is null or d.recordDate >= :from)
            and (:to is null or d.recordDate <= :to)
            order by d.recordDate desc
            """)
    Page<DailyReturnHistoryEntity> search(
            @Param("username") String username,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
