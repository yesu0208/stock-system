package arile.toy.stocksystem.accountserver.rank.repository;

import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RankHistoryRepository extends JpaRepository<RankHistoryEntity, Long> {

    Page<RankHistoryEntity> findByUsernameOrderByRecordDateDesc(String username, Pageable pageable);

    boolean existsByUsernameAndRecordDate(String username, LocalDate recordDate);
}
