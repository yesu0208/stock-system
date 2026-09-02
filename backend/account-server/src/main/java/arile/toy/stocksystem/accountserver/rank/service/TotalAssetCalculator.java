package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
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
    private final LeveragePositionRepository leveragePositionRepository;
    private final StockSummaryRedisRepository stockSummaryRedisRepository;

    /** 현금 + 현물 평가금액 + 레버리지 포지션 순자산(평가금액-대출금) */
    public long calculate(String username, long cashBalance) {

        long stockValue = calculateSpotStockValue(username);
        long leverageNetValue = calculateLeverageNetValue(username);

        return cashBalance + stockValue + leverageNetValue;
    }

    private long calculateSpotStockValue(String username) {

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

        return stockValue;
    }

    /**
     * 레버리지 포지션의 순자산(평가금액 - 대출금) 합산.
     * 대출금은 부채이므로 총자산 산정 시 반드시 차감해야 등급(RP) 계산이 실제 순자산을 반영한다.
     * 평가금액이 대출금보다 작아지면(반대매매 미실행 구간) 음수가 되어 총자산을 깎는 것이 맞다.
     */
    private long calculateLeverageNetValue(String username) {

        List<LeveragePositionEntity> positions = leveragePositionRepository.findByUsername(username);

        long netValue = 0L;
        for (LeveragePositionEntity position : positions) {
            var summary = stockSummaryRedisRepository.findByStockCode(position.getStockCode());
            if (summary == null || summary.curPrice() == null) {
                log.warn("No price found for stockCode={}, skip leverage position in total asset calc. username={}",
                        position.getStockCode(), username);
                continue;
            }

            long evaluationAmount = (long) position.getQuantity() * summary.curPrice();
            netValue += evaluationAmount - position.getLoanAmount();
        }

        return netValue;
    }
}
