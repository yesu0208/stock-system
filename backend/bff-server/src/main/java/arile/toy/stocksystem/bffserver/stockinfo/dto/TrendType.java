package arile.toy.stocksystem.bffserver.stockinfo.dto;

public enum TrendType {

    TIME("time"),
    DAY("day");

    private final String path;

    TrendType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
