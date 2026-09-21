package arile.toy.stocksystem.accountserver.leverage.dto;

public record LeveragePositionInfo(
        Integer quantity,
        Integer availableQuantity,
        Long purchaseAmount,
        Long costAmount,
        Long loanAmount,
        String marginStatus
) {
    public static LeveragePositionInfo of(Integer quantity, Integer availableQuantity,
                                          Long purchaseAmount, Long costAmount, Long loanAmount, String marginStatus) {
        return new LeveragePositionInfo(quantity, availableQuantity, purchaseAmount, costAmount, loanAmount, marginStatus);
    }
}
