package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.common.util.TickSizeCalculator;

public final class TrailingStopPriceCalculator {

    private TrailingStopPriceCalculator() {
    }

    public static int calcTrigger(TrailingStopType type, int basePrice, double stopPercent) {
        double multiplier = stopPercent / 100.0;
        double raw = type == TrailingStopType.BUY
                ? basePrice * (1 + multiplier)
                : basePrice * (1 - multiplier);
        int rounded = (int) Math.round(raw);

        return type == TrailingStopType.BUY
                ? TickSizeCalculator.ceilToTick(rounded)
                : TickSizeCalculator.floorToTick(rounded);
    }
}
