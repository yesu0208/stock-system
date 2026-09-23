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

        // 0~10% 구간: 0.01%당 1점 (1%당 100점)
        score += Math.min(rate, 10.0) * 100.0;

        // 10~20% 구간: 10% 초과분에 대해 0.01%당 0.7점 (1%당 70점)
        if (rate > 10.0) {
            score += (Math.min(rate, 20.0) - 10.0) * 70.0;
        }

        // 20% 초과 구간: 20% 초과분에 대해 0.01%당 0.5점 (1%당 50점)
        if (rate > 20.0) {
            score += (rate - 20.0) * 50.0;
        }

        return Math.round(score);
    }

    private long calculateNegativeScore(double absRate) {
        double score = 0;

        // 0~10% 손실 구간: 0.01%당 0.9점 차감 (1%당 90점)
        score += Math.min(absRate, 10.0) * 90.0;

        // 10~20% 손실 구간: 10% 초과분에 대해 0.01%당 0.7점 차감 (1%당 70점)
        if (absRate > 10.0) {
            score += (Math.min(absRate, 20.0) - 10.0) * 70.0;
        }

        // 20% 초과 손실 구간: 20% 초과분에 대해 0.01%당 0.5점 차감 (1%당 50점)
        if (absRate > 20.0) {
            score += (absRate - 20.0) * 50.0;
        }

        return -(long) Math.round(score);
    }
}
