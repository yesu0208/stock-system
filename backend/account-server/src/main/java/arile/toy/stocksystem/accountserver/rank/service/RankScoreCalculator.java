package arile.toy.stocksystem.accountserver.rank.service;

import org.springframework.stereotype.Component;

@Component
public class RankScoreCalculator {

    private static final long TRADE_AMOUNT_CAP = 300_000_000L;
    private static final long TRADE_POINT_UNIT = 10_000_000L;
    private static final double TRADE_POINT_MAX = 30L;
    
    public double calculateDailyDelta(long previousDayTotalAsset, long todayTotalAsset, long dailyTradeAmount) {

        double profitRate = previousDayTotalAsset == 0
                ? 0.0
                : (todayTotalAsset - previousDayTotalAsset) * 100.0 / previousDayTotalAsset;

        double profitScore = calculateProfitScore(profitRate);
        double tradePoint = calculateTradePoint(dailyTradeAmount);

        return profitScore + tradePoint;
    }

    private double calculateTradePoint(long dailyTradeAmount) {
        long recognizedAmount = Math.min(dailyTradeAmount, TRADE_AMOUNT_CAP);
        long point = recognizedAmount / TRADE_POINT_UNIT;
        return Math.min(point, TRADE_POINT_MAX);
    }

    private double calculateProfitScore(double ratePercent) {
        if (ratePercent >= 0) {
            return calculatePositiveScore(ratePercent);
        } else {
            return calculateNegativeScore(-ratePercent); // 절댓값으로 계산 후 마지막에 부호 반전
        }
    }

    private long calculatePositiveScore(double rate) {
        double score = 0;

        double smallBand = Math.min(rate, 2.0);
        score += smallBand * 6.0;

        if (rate > 10.0) {
            double midBand = Math.min(rate, 5.0) - 2.0;
            score += midBand * 4.0;
        }

        if (rate > 20.0) {
            double largeBand = rate - 5.0;
            score += largeBand * 2.0;
        }

        return Math.round(score);
    }

    private long calculateNegativeScore(double absRate) {
        double score = 0;

        double smallBand = Math.min(absRate, 2.0);
        score += smallBand * 4.0;

        if (absRate > 10.0) {
            double midBand = Math.min(absRate, 5.0) - 2.0;
            score += midBand * 2.5;
        }

        if (absRate > 20.0) {
            double largeBand = absRate - 5.0;
            score += largeBand * 1.2;
        }

        return -(long) Math.round(score);
    }
}
