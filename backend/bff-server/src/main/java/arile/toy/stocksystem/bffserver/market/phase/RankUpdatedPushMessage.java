package arile.toy.stocksystem.bffserver.market.phase;

public record RankUpdatedPushMessage(String message) {
    private static final RankUpdatedPushMessage INSTANCE =
            new RankUpdatedPushMessage("랭크 점수(RP)와 당일 수익률 정산이 반영되었습니다.");

    public static RankUpdatedPushMessage of() {
        return INSTANCE;
    }
}
