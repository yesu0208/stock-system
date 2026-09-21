package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.springframework.stereotype.Component;

@Component
public class ReserveAmountCalculator {

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

    public long calculateReserveAmount(LeverageRatio leverageRatio, long orderAmount) {
        long principal = leverageRatio.isSpot() ? orderAmount : leverageRatio.calculateMarginDeposit(orderAmount);
        long fee = calculateFee(orderAmount);
        return principal + fee;
    }
}
