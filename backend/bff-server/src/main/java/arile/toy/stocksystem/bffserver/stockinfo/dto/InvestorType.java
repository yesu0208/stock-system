package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum InvestorType {
    FOREIGN("9000"), INSTITUTION("1000");

    private final String code;
    InvestorType(String code) { this.code = code; }
    public String getCode() { return code; }
}
