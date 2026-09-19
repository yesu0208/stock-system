package arile.toy.stocksystem.bffserver.market.phase;

public record InterestAppliedPushMessage(String message) {
    private static final InterestAppliedPushMessage INSTANCE =
            new InterestAppliedPushMessage("신용거래 이자가 반영되었습니다.");

    public static InterestAppliedPushMessage of() {
        return INSTANCE;
    }
}
