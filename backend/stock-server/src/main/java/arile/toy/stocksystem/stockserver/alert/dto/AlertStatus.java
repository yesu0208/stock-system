package arile.toy.stocksystem.stockserver.alert.dto;

import lombok.Getter;

@Getter
public enum AlertStatus {
    ACTIVE(true),
    FIRED(false),
    CANCELED(false);

    private final boolean open;

    AlertStatus(boolean open) {
        this.open = open;
    }
}
