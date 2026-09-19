package arile.toy.stocksystem.stockserver.otoco.dto;

public enum OtocoResultCode {
    INSUFFICIENT_BALANCE,
    INTERNAL_ERROR,
    INVALID_TP_PRICE,
    INVALID_SL_PRICE,
    ENTRY_TRIGGERED,
    ENTRY_PARTIALLY_FILLED,
    ENTRY_FILLED,
    ENTRY_FAILED,
    ENTRY_CANCELED,
    TP_TRIGGERED,
    SL_TRIGGERED
}
