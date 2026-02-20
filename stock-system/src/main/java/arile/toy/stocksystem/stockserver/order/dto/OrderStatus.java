package arile.toy.stocksystem.stockserver.order.dto;

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
