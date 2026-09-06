package arile.toy.stocksystem.bffserver.otococancel.dto;

public enum OtocoCancelErrorCode {
    ALREADY_CANCELLED("[취소 거절(OTOCO)] 이미 취소된 주문입니다."),
    ALREADY_COMPLETED("[취소 거절(OTOCO)] 이미 완료된 주문입니다."),
    INTERNAL_ERROR("[취소 거절] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final String userMessage;

    OtocoCancelErrorCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
