package arile.toy.stocksystem.bffserver.alertcancel.dto;

public enum AlertCancelErrorCode {
    ALREADY_CANCELLED("[알림 해제 거절] 이미 해제된 알림입니다."),
    ALREADY_FIRED("[알림 해제 거절] 이미 발동된 알림입니다."),
    INTERNAL_ERROR("[알림 해제 거절] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final String userMessage;

    AlertCancelErrorCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
