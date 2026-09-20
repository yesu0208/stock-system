package arile.toy.stocksystem.bffserver.order.dto;

public enum OrderOrigin {
    MANUAL,
    AUTO_ORDER,
    OTOCO_ENTRY,
    OTOCO_TAKE_PROFIT,
    OTOCO_STOP_LOSS,
    TRAILING_STOP
}
