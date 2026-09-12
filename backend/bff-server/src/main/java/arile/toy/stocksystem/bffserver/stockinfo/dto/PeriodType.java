package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum PeriodType {
    DAY("DAY"),
    WEEK("WEEK"),
    MONTH("MONTH"),
    THREE_MONTH("THREE_MONTH");

    private final String code;

    PeriodType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
