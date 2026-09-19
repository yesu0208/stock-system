package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.common.util.TickSizeCalculator;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoExitMode;

public final class OtocoPriceResolver {

    private OtocoPriceResolver() {
    }

    public static Integer resolveTakeProfit(Integer entryTriggerPrice, OtocoExitMode mode, Integer price, Double pct) {
        if (mode == OtocoExitMode.PRICE) {
            return price;
        }
        int raw = (int) Math.round(entryTriggerPrice * (1 + pct / 100.0));
        return TickSizeCalculator.ceilToTick(raw);
    }

    public static Integer resolveStopLoss(Integer entryTriggerPrice, OtocoExitMode mode, Integer price, Double pct) {
        if (mode == OtocoExitMode.PRICE) {
            return price;
        }
        int raw = (int) Math.round(entryTriggerPrice * (1 - pct / 100.0));
        return TickSizeCalculator.floorToTick(raw);
    }
}
