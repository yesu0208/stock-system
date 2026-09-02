package arile.toy.stocksystem.stockserver.order.dto;

public enum LeverageRatio {
    SPOT(1.0, 1.0),
    X1_5(1.5, 0.667),
    X2(2.0, 0.5),
    X2_5(2.5, 0.4);

    private final double ratio;
    private final double marginRate;

    LeverageRatio(double ratio, double marginRate) {
        this.ratio = ratio;
        this.marginRate = marginRate;
    }

    public boolean isSpot() {
        return this == SPOT;
    }

    public long calculateMarginDeposit(long purchaseAmount) {
        return Math.round(purchaseAmount * marginRate);
    }

    public long calculateLoanAmount(long purchaseAmount) {
        return purchaseAmount - calculateMarginDeposit(purchaseAmount);
    }
}
