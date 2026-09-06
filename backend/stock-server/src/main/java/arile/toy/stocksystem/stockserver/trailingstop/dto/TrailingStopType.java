package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.order.dto.OrderType;

public enum TrailingStopType {
    BUY,
    SELL;

    public OrderType toOrderType() {
        return switch (this) {
            case BUY -> OrderType.BUY;
            case SELL -> OrderType.SELL;
        };
    }
}
