package arile.toy.stocksystem.accountserver.leverage.service;

import org.springframework.stereotype.Component;

@Component
public class LeverageInterestCalculator {

    /** 연이율 8.5% */
    private static final double ANNUAL_INTEREST_RATE = 0.085;
    private static final int DAYS_IN_YEAR = 365;

    /** 대출금 × (8.5% / 365), 원 단위 반올림 */
    public long calculateDailyInterest(long loanAmount) {
        if (loanAmount <= 0) {
            return 0L;
        }
        return Math.round(loanAmount * ANNUAL_INTEREST_RATE / DAYS_IN_YEAR);
    }
}
