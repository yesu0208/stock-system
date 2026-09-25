package arile.toy.stocksystem.bffserver.watchlist.service;

import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListNotFoundException;
import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;
import arile.toy.stocksystem.bffserver.watchlist.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        // 개수가 아니라 가장 큰 순서 + 1 (중간 종목 삭제 후 추가 시 순서가 겹치지 않도록)
        int nextOrder = watchListRepository.findMaxSortOrder(username) + 1;

        try {
            return watchListRepository.save(
                    WatchListEntity.of(username, stockCode, stockName, nextOrder)
            );
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 종목을 추가해 유니크 제약에 걸린 경우 (존재 확인과 저장 사이의 경합)
            throw new WatchListAlreadyExistsException(stockCode);
        }
    }

    @Transactional
    public void remove(String username, String stockCode) {

        if (!watchListRepository.existsByUsernameAndStockCode(username, stockCode)) {
            throw new WatchListNotFoundException(stockCode);
        }

        watchListRepository.deleteByUsernameAndStockCode(username, stockCode);
    }
}
