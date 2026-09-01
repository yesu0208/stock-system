package arile.toy.stocksystem.stockserver.order.dto;

public enum LeverageRatio {
    SPOT, X1_5, X2, X2_5;

    public boolean isSpot() {
        return this == SPOT;
    }
}
