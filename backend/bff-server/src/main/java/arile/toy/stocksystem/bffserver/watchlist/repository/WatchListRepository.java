package arile.toy.stocksystem.bffserver.watchlist.repository;

import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
