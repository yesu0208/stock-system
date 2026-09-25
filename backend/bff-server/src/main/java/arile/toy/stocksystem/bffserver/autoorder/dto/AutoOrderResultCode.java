package arile.toy.stocksystem.bffserver.autoorder.dto;

public enum AutoOrderResultCode {
    INSUFFICIENT_BALANCE("[자동 주문 거절] 잔액이 부족하여 자동 주문에 실패했습니다."),
    INSUFFICIENT_STOCK("[자동 주문 거절] 보유 주식이 부족하여 자동 주문에 실패했습니다."),
    INTERNAL_ERROR("[자동 주문 거절] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    TRIGGER_FAILED("[자동 주문 발동 실패] 발동 조건에 도달했지만 주문 접수 중 오류가 발생해 자동 주문이 취소되었습니다. 예약된 금액·주식은 반환됩니다."),
    TRIGGERED("주문 triggered.");
    private final String userMessage;

    AutoOrderResultCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
