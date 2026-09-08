package arile.toy.stocksystem.bffserver.dailyreturn.dto;

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
}
