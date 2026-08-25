package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record UpjongStock(
        String name,
        String code,
        String price,
        String change,
        String rate
) {
}
