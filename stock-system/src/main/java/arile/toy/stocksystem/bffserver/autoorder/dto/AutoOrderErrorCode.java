package arile.toy.stocksystem.bffserver.autoorder.dto;

public enum AutoOrderErrorCode {
    INSUFFICIENT_BALANCE("[자동 주문 거절] 잔액이 부족하여 자동 주문에 실패했습니다."),
    INTERNAL_ERROR("[자동 주문 거절] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final String userMessage;

    AutoOrderErrorCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
