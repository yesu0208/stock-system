package arile.toy.stocksystem.bffserver.account.dto;

public record LeveragePositionInfo(
        Integer quantity,
        Integer availableQuantity,
        Long purchaseAmount,
        Long loanAmount
) {
}
