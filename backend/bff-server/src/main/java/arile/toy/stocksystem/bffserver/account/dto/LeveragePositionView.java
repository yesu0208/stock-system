package arile.toy.stocksystem.bffserver.account.dto;

public record LeveragePositionView(
        String stockCode,
        String leverageRatio,
        Integer quantity,
        Integer availableQuantity,
        Long purchaseAmount,
        Long costAmount,
        Long loanAmount,
        Long evaluationAmount,
        Long netValue,
        Long profitAmount,
        Double profitRate,
        Integer currentPrice,
        String marginStatus,
        Long initialMargin, // 개시증거금 = purchaseAmount - loanAmount
        Long maintenanceMargin, // 유지증거금 = loanAmount × 유지증거금비율
        Long maintenancePrice // 유지증거금 기준가 = (유지증거금비율 × loanAmount) / quantity
) {
}
