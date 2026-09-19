package arile.toy.stocksystem.bffserver.market.phase;

public record MarketClosePushMessage(String session, String message) {
    public static MarketClosePushMessage of(String session) {
        String message = "AFTER".equals(session)
                ? "애프터마켓 미체결 내역 정리 및 정산이 완료되었습니다."
                : "정규장 미체결 내역 정리 및 정산이 완료되었습니다.";
        return new MarketClosePushMessage(session, message);
    }
}
