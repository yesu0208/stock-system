package arile.toy.stocksystem.accountserver.leverage.dto;

public record LeveragePositionInfo(
        Integer quantity,
        Integer availableQuantity,
        Long purchaseAmount,
        Long loanAmount
) {
    public static LeveragePositionInfo of(Integer quantity, Integer availableQuantity, Long purchaseAmount, Long loanAmount) {
        return new LeveragePositionInfo(quantity, availableQuantity, purchaseAmount, loanAmount);
    }
}
