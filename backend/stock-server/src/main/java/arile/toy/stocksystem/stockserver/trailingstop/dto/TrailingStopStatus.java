package arile.toy.stocksystem.stockserver.trailingstop.dto;

import lombok.Getter;

@Getter
public enum TrailingStopStatus {
    ACTIVE(true),
    TRIGGERED(false),
    CANCELED(false);

    private final boolean open;

    TrailingStopStatus(boolean open) {
        this.open = open;
    }
}
