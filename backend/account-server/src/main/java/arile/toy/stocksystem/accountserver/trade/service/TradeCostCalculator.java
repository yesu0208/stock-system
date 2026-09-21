package arile.toy.stocksystem.accountserver.trade.service;

import org.springframework.stereotype.Component;

@Component
public class TradeCostCalculator {

    private static final double FEE_RATE = 0.00015;
    private static final double TAX_RATE = 0.0020;

    public long calculateFee(long amount) {
        if (amount <= 0) {
            return 0L;
        }
        return Math.round(amount * FEE_RATE);
    }

    public long calculateTax(long amount) {
        if (amount <= 0) {
            return 0L;
        }
        return Math.round(amount * TAX_RATE);
    }
}
