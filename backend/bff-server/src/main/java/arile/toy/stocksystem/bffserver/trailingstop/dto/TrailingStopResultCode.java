package arile.toy.stocksystem.bffserver.trailingstop.dto;

public enum TrailingStopResultCode {
    INSUFFICIENT_BALANCE("[트레일링 스탑 거절] 잔액이 부족하여 등록에 실패했습니다."),
    INSUFFICIENT_STOCK("[트레일링 스탑 거절] 보유 주식이 부족하여 등록에 실패했습니다."),
    INTERNAL_ERROR("[트레일링 스탑 거절] 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    TRIGGERED("트레일링 스탑이 발동되었습니다."),
    TRAILING_UPDATED("트레일링 스탑 기준가가 갱신되었습니다.");

    private final String userMessage;

    TrailingStopResultCode(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
