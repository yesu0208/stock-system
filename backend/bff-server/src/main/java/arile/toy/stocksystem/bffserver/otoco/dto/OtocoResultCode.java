package arile.toy.stocksystem.bffserver.otoco.dto;

public enum OtocoResultCode {
    INSUFFICIENT_BALANCE("[OTOCO 거절] 잔액이 부족하여 등록에 실패했습니다."),
    INTERNAL_ERROR("[OTOCO 거절] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    ENTRY_TRIGGERED("진입 조건이 충족되어 주문이 접수되었습니다."),
    ENTRY_FILLED("진입 주문이 체결되어 TP/SL 감시를 시작합니다."),
    ENTRY_FAILED("[OTOCO 거절] 진입 처리 중 오류가 발생했습니다."),
    TP_TRIGGERED("익절(TP) 조건이 충족되어 청산되었습니다."),
    SL_TRIGGERED("손절(SL) 조건이 충족되어 청산되었습니다.");

    private final String userMessage;

    OtocoResultCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
