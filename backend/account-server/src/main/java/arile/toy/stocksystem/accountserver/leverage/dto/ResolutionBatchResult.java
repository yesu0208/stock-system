package arile.toy.stocksystem.accountserver.leverage.dto;

public record ResolutionBatchResult(
        int recovered,
        int suspended
) {
}
