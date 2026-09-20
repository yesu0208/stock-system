package arile.toy.stocksystem.stockserver.order.dto;

public enum OrderOrigin {
    MANUAL, // 사용자가 직접 주문
    AUTO_ORDER, // 자동주문(지정가 감시) 발동
    OTOCO_ENTRY, // OTOCO 진입
    OTOCO_TAKE_PROFIT, // OTOCO 익절
    OTOCO_STOP_LOSS, // OTOCO 손절
    TRAILING_STOP // 트레일링 스탑 발동
}
