package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum DealType {
    BUY("buy"), SELL("sell");

    private final String code;
    DealType(String code) { this.code = code; }
    public String getCode() { return code; }
}
