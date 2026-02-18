package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import lombok.Getter;

@Getter
public enum AutoOrderStatus {
    ACTIVE(true),
    TRIGGERED(false),
    CANCELED(false);

    private final boolean open;

    AutoOrderStatus(boolean open) {
        this.open = open;
    }
}
