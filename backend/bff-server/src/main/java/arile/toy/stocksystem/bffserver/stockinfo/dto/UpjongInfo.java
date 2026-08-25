package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record UpjongInfo(
        String name,
        String no,
        String changeRate,
        String total,
        String rise,
        String steady,
        String fall,
        String graphRatio
) {
}
