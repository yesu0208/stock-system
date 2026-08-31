package arile.toy.stocksystem.bffserver.watchlist.service;

import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListNotFoundException;
import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;
import arile.toy.stocksystem.bffserver.watchlist.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchListService {

    private final WatchListRepository watchListRepository;

    public List<WatchListEntity> getAll(String username) {
        return watchListRepository.findByUsernameOrderBySortOrderAsc(username);
    }

    @Transactional
    public WatchListEntity add(String username, String stockCode, String stockName) {

        if (watchListRepository.existsByUsernameAndStockCode(username, stockCode)) {
            throw new WatchListAlreadyExistsException(stockCode);
        }

        int nextOrder = (int) watchListRepository.countByUsername(username);

        return watchListRepository.save(
                WatchListEntity.of(username, stockCode, stockName, nextOrder)
        );
    }

    @Transactional
    public void remove(String username, String stockCode) {

        if (!watchListRepository.existsByUsernameAndStockCode(username, stockCode)) {
            throw new WatchListNotFoundException(stockCode);
        }

        watchListRepository.deleteByUsernameAndStockCode(username, stockCode);
    }
}
