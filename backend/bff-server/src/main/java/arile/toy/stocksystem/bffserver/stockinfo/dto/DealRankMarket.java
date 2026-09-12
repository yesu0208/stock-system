package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum DealRankMarket {
    KOSPI("KOSPI"), KOSDAQ("KOSDAQ");

    private final String code;
    DealRankMarket(String code) { this.code = code; }
    public String getCode() { return code; }
}
