package arile.toy.stocksystem.accountserver.userstock.service;

import arile.toy.stocksystem.accountserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import arile.toy.stocksystem.accountserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.accountserver.userstock.repository.UserStockRepository;
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
    private final UserAccountRepository userAccountRepository;
    private final UserAccountRedisRepository userAccountRedisRepository;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    @Transactional
    public void settleStocks(Set<String> usernames) {

        for (String username : usernames) {

            List<UserStockEntity> userStockEntities = userStockRepository.findByUsername(username);

            Map<String, StockInfo> redisStocks = userAccountRedisRepository.getStocks(username);

            if (redisStocks == null) {
                log.error("Redis stocks is null. username={}", username);
                redisStocks = new HashMap<>();
            }

            Map<String, StockInfo> stocksMap = new HashMap<>();

            for (UserStockEntity entity : userStockEntities) {

                String stockCode = entity.getStockCode();
                int quantity = entity.getQuantity();
                long totalAmount = entity.getAmount();

                StockInfo redisInfo = redisStocks.get(stockCode);

                if (redisInfo == null ||
                        redisInfo.quantity() != quantity ||
                        redisInfo.totalAmount() == null ||
                        redisInfo.totalAmount() != totalAmount) {

                    log.warn("Stock {} out of sync for user {}. DB: {}:{}, Redis: {}",
                            stockCode, username, quantity, totalAmount, redisInfo);
                }

                stocksMap.put(stockCode,
                        StockInfo.of(quantity, quantity, totalAmount));
            }

            for (String redisStockCode : redisStocks.keySet()) {
                if (!stocksMap.containsKey(redisStockCode)) {
                    log.warn("Redis has extra stock {} for user {}",
                            redisStockCode, username);
                }
            }

            userAccountRedisRepository.saveStocks(username, stocksMap);
            accountUpdateEventPublisher.publish(username);
            log.info("Redis stocks updated for user {}", username);
        }
    }

    @Transactional
    public void settleAllStocks() {
        Set<String> allUsernames = new HashSet<>(userAccountRepository.findAllUsernames());
        settleStocks(allUsernames);
    }
}
