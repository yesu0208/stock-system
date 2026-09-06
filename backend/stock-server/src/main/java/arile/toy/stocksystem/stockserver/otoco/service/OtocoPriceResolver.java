package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoExitMode;

public final class OtocoPriceResolver {

    private OtocoPriceResolver() {
    }

    public static Integer resolveTakeProfit(Integer entryTriggerPrice, OtocoExitMode mode, Integer price, Double pct) {
        if (mode == OtocoExitMode.PRICE) {
            return price;
        }
        return (int) Math.round(entryTriggerPrice * (1 + pct / 100.0));
    }

    public static Integer resolveStopLoss(Integer entryTriggerPrice, OtocoExitMode mode, Integer price, Double pct) {
        if (mode == OtocoExitMode.PRICE) {
            return price;
        }
        return (int) Math.round(entryTriggerPrice * (1 - pct / 100.0));
    }
}
