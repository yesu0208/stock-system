package arile.toy.stocksystem.stockserver.market.phase;

public enum StockServerMarketPhase {
    MORNING_CALL,  // 08:50:05 ~ 09:00:00 아침 동시호가
    OPEN,          // 09:00:00 ~ 15:20:00 정규장
    CLOSING_CALL,  // 15:20:00 ~ 15:30:00 마감 동시호가
    AFTER,         // 16:00:05 ~ 20:00:00 애프터
    CLOSED;        // 나머지 (15:30~16:00:05 포함)

    // 기존 "OPEN"의 의미 — MORNING_CALL/OPEN/CLOSING_CALL/AFTER는 모두
    // "거래 가능한 상태(주문 접수 가능)"로 간주하고, CLOSED만 아님.
    public boolean isOrderable() {
        return this != CLOSED;
    }
}
