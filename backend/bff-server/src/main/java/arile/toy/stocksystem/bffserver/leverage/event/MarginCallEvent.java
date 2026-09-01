package arile.toy.stocksystem.bffserver.leverage.event;

public record MarginCallEvent(
        String username,
        String stockCode,
        String leverageRatio,
        String status,
        Double marginRatio
) {
}
