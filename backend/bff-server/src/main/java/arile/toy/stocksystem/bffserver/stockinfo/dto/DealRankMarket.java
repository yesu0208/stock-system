package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum DealRankMarket {
    KOSPI("01"), KOSDAQ("02");

    private final String code;
    DealRankMarket(String code) { this.code = code; }
    public String getCode() { return code; }
}
