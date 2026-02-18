package arile.toy.stocksystem.stockserver.userstock.service;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.StockServerAccountRepository;
import arile.toy.stocksystem.stockserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.stockserver.userstock.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStockService {

    private final UserStockRepository userStockRepository;
    private final StockServerAccountRepository stockServerAccountRepository;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    @Transactional
    public void settleStocks(Set<String> usernames) {

        for (String username : usernames) {

            List<UserStockEntity> userStockEntities = userStockRepository.findByUsername(username);

            Map<String, StockInfo> redisStocks = stockServerAccountRepository.getStocks(username);

            Map<String, StockInfo> stocksMap = new HashMap<>();

            for (UserStockEntity entity : userStockEntities) {

                String stockCode = entity.getStockCode();
                int quantity = entity.getQuantity();
                int buyPrice = quantity != 0
                        ? (int) (entity.getAmount() / quantity)
                        : 0;

                StockInfo redisInfo = redisStocks.get(stockCode);

                if (redisInfo == null ||
                        redisInfo.quantity() != quantity ||
                        redisInfo.buyPrice() != buyPrice) {

                    log.warn("Stock {} out of sync for user {}. DB: {}:{}, Redis: {}",
                            stockCode, username, quantity, buyPrice, redisInfo);
                }

                stocksMap.put(stockCode,
                        StockInfo.of(quantity, buyPrice, quantity));
            }

            for (String redisStockCode : redisStocks.keySet()) {
                if (!stocksMap.containsKey(redisStockCode)) {
                    log.warn("Redis has extra stock {} for user {}",
                            redisStockCode, username);
                }
            }

            stockServerAccountRepository.saveStocks(username, stocksMap);
            accountUpdateEventPublisher.publish(username);
            log.info("Redis stocks updated for user {}", username);
        }
    }
}
