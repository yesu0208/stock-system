package arile.toy.stocksystem.bffserver.alert.dto;

public enum AlertErrorCode {
    INTERNAL_ERROR("[알림 등록 실패] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final String userMessage;

    AlertErrorCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
