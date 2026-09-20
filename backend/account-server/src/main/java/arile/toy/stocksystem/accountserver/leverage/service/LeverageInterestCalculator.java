package arile.toy.stocksystem.accountserver.leverage.service;

import org.springframework.stereotype.Component;

@Component
public class LeverageInterestCalculator {

    /** 연이율 8.5% */
    private static final double ANNUAL_INTEREST_RATE = 0.085;
    private static final int DAYS_IN_YEAR = 365;

    /**
     * 대출금 × (8.5% / 365) × 경과일수, 원 단위 반올림.
     * 연이율을 365(달력일 전체)로 나누도록 설계되어 있으므로, 주말/휴장일에도
     * 이자가 계속 발생.
     */
    public long calculateInterest(long loanAmount, long days) {
        if (loanAmount <= 0 || days <= 0) {
            return 0L;
        }
        return Math.round(loanAmount * ANNUAL_INTEREST_RATE / DAYS_IN_YEAR * days);
    }

    /** 정확히 하루치만 필요할 때 사용 */
    public long calculateDailyInterest(long loanAmount) {
        return calculateInterest(loanAmount, 1);
    }
}
