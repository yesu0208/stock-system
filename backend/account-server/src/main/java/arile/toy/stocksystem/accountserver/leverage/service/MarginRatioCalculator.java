package arile.toy.stocksystem.accountserver.leverage.service;

import org.springframework.stereotype.Component;

@Component
public class MarginRatioCalculator {

    /** 최소유지 담보비율 140% */
    private static final double MIN_MAINTENANCE_RATIO = 140.0;

    /** 담보비율 = 평가금액 / 대출금 × 100 */
    public double calculateRatio(long evaluationAmount, long loanAmount) {
        if (loanAmount <= 0) {
            return Double.MAX_VALUE; // 대출금 없으면 위험 없음
        }
        return evaluationAmount * 100.0 / loanAmount;
    }

    public boolean isBelowMaintenance(double ratio) {
        return ratio < MIN_MAINTENANCE_RATIO;
    }
}
