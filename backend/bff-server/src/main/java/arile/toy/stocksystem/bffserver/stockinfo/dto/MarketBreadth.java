package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record MarketBreadth(
        String upperLimit,
        String rise,
        String steady,
        String fall,
        String lowerLimit,
        String basis
) {
}
