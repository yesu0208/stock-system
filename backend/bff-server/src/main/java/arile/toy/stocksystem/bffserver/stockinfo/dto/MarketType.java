package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum MarketType {

    KOSPI("KOSPI"),
    KOSDAQ("KOSDAQ"),
    FUTURES("FUT");

    private final String code;

    MarketType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
