package arile.toy.stocksystem.stockserver.autoorder.dto;

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
