package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import arile.toy.stocksystem.accountserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.accountserver.userstock.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TotalAssetCalculator {

    private final UserStockRepository userStockRepository;
    private final StockSummaryRedisRepository stockSummaryRedisRepository;

    /** 현금 + 보유주식 평가금액(현재가 기준) */
    public long calculate(String username, long cashBalance) {

        List<UserStockEntity> stocks = userStockRepository.findByUsername(username);

        long stockValue = 0L;
        for (UserStockEntity stock : stocks) {
            var summary = stockSummaryRedisRepository.findByStockCode(stock.getStockCode());
            if (summary == null || summary.curPrice() == null) {
                log.warn("No price found for stockCode={}, skip in total asset calc. username={}",
                        stock.getStockCode(), username);
                continue;
            }
            stockValue += (long) stock.getQuantity() * summary.curPrice();
        }

        return cashBalance + stockValue;
    }
}
