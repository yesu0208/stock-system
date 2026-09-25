package arile.toy.stocksystem.bffserver.watchlist.repository;

import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchListRepository extends JpaRepository<WatchListEntity, Long> {

    List<WatchListEntity> findByUsernameOrderBySortOrderAsc(String username);

    Optional<WatchListEntity> findByUsernameAndStockCode(String username, String stockCode);

    boolean existsByUsernameAndStockCode(String username, String stockCode);

    void deleteByUsernameAndStockCode(String username, String stockCode);

    long countByUsername(String username);

    /** 사용자의 가장 큰 순서 번호 (관심종목이 없으면 -1) */
    @Query("select coalesce(max(w.sortOrder), -1) from WatchListEntity w where w.username = :username")
    int findMaxSortOrder(@Param("username") String username);
}
