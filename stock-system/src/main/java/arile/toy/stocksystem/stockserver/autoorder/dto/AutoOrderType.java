package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.order.dto.OrderType;

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
