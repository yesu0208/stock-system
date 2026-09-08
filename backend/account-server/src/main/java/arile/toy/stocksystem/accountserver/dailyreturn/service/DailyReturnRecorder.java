package arile.toy.stocksystem.accountserver.dailyreturn.service;

import arile.toy.stocksystem.accountserver.dailyreturn.entity.DailyReturnHistoryEntity;
import arile.toy.stocksystem.accountserver.dailyreturn.repository.DailyReturnHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReturnRecorder {

    private final DailyReturnHistoryRepository dailyReturnHistoryRepository;

    @Value("${account.initial-balance}")
    private long initialBalance;

    public void record(String username, LocalDate recordDate, long previousDayTotalAsset,
                       long todayTotalAsset, long dailyTradeAmount) {

        if (dailyReturnHistoryRepository.existsByUsernameAndRecordDate(username, recordDate)) {
            log.warn("Daily return already recorded. username={}, date={}. Skip.", username, recordDate);
            return;
        }

        long dailyProfitAmount = todayTotalAsset - previousDayTotalAsset;
        double dailyProfitRate = previousDayTotalAsset == 0
                ? 0.0
                : dailyProfitAmount * 100.0 / previousDayTotalAsset;

        long cumulativeProfitAmount = todayTotalAsset - initialBalance;
        double cumulativeProfitRate = (double) cumulativeProfitAmount / initialBalance * 100;

        dailyReturnHistoryRepository.save(
                DailyReturnHistoryEntity.of(
                        username, recordDate, previousDayTotalAsset, todayTotalAsset,
                        dailyProfitAmount, dailyProfitRate, dailyTradeAmount,
                        cumulativeProfitAmount, cumulativeProfitRate)
        );
    }
}
