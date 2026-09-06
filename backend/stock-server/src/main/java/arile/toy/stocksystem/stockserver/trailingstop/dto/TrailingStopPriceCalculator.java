package arile.toy.stocksystem.stockserver.trailingstop.dto;

public final class TrailingStopPriceCalculator {

    private TrailingStopPriceCalculator() {
    }

    public static int calcTrigger(TrailingStopType type, int basePrice, double stopPercent) {
        double multiplier = stopPercent / 100.0;
        return type == TrailingStopType.BUY
                ? (int) Math.round(basePrice * (1 + multiplier))
                : (int) Math.round(basePrice * (1 - multiplier));
    }
}
