package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record PopularStock(
        int rank,
        String code,
        String name,
        String price,
        String direction
) {
}
