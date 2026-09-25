package arile.toy.stocksystem.stockserver.autoorder.dto;

public enum AutoOrderResultCode {
    INSUFFICIENT_BALANCE,
    INSUFFICIENT_STOCK,
    INTERNAL_ERROR,
    TRIGGER_FAILED,
    TRIGGERED
}
