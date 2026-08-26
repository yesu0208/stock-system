package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum MarketType {

    KOSPI("01"),
    KOSDAQ("02"),
    FUTURES("03");

    private final String code;

    MarketType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
