package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import arile.toy.stocksystem.stockserver.trading.dto.order.OrderType;

public enum AutoOrderType {
    BUY,
    SELL;

    public OrderType toOrderType() {
        return switch (this) {
            case BUY  -> OrderType.BUY;
            case SELL -> OrderType.SELL;
        };
    }
}
