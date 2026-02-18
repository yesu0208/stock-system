package arile.toy.stocksystem.stockserver.trading.dto.order;

import lombok.Getter;

@Getter
public enum OrderStatus {

    OPEN(true),
    PARTIAL(true),
    FILLED(false),
    CANCELED(false);

    private final boolean open;

    OrderStatus(boolean open) {
        this.open = open;
    }

}
