package arile.toy.stocksystem.bffserver.account.dto;

public record LeveragePositionView(
        String stockCode,
        String leverageRatio,
        Integer quantity,
        Integer availableQuantity,
        Long purchaseAmount,
        Long loanAmount,
        Long evaluationAmount,
        Long netValue,
        Double profitRate,
        Integer currentPrice
) {
}
