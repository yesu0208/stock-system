package arile.toy.stocksystem.accountserver.dailyreturn.dto;

import arile.toy.stocksystem.accountserver.dailyreturn.entity.DailyReturnHistoryEntity;

import java.time.LocalDate;

public record DailyReturnHistoryItem(
        LocalDate date,
        Long previousDayTotalAsset,
        Long totalAsset,
        Long dailyProfitAmount,
        Double dailyProfitRate,
        Long dailyTradeAmount,
        Long cumulativeProfitAmount,
        Double cumulativeProfitRate
) {
    public static DailyReturnHistoryItem fromEntity(DailyReturnHistoryEntity entity) {
        return new DailyReturnHistoryItem(
                entity.getRecordDate(), entity.getPreviousDayTotalAsset(), entity.getTotalAsset(),
                entity.getDailyProfitAmount(), entity.getDailyProfitRate(), entity.getDailyTradeAmount(),
                entity.getCumulativeProfitAmount(), entity.getCumulativeProfitRate()
        );
    }
}
