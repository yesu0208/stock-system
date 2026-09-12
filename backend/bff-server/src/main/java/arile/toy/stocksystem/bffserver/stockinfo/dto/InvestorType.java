package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum InvestorType {
    FOREIGN("FOREIGNER"), INSTITUTION("ORGANIZATION");

    private final String code;
    InvestorType(String code) { this.code = code; }
    public String getCode() { return code; }
}
