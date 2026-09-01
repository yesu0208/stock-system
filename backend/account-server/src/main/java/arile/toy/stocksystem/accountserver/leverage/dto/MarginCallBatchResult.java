package arile.toy.stocksystem.accountserver.leverage.dto;

public record MarginCallBatchResult(
        int newMarginCalls,
        int recovered,
        int queuedForLiquidation) {
}
