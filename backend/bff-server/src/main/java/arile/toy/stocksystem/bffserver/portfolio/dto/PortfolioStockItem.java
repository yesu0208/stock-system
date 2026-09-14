package arile.toy.stocksystem.bffserver.portfolio.dto;

public record PortfolioStockItem(
        String stockCode,

        Long spotAmount,  // 현물 평가금액
        Long leverageAmount, // 레버리지 평가금액
        Long totalAmount, // 합산 평가금액
        Double ratioInSector,
        Double ratioInTotal,
        Long profitAmount, // 합산 손익금
        Double profitRate, // 합산 수익률

        Long spotBuyAmount, // 현물 매수(매입)금액
        Long spotProfitAmount, // 현물 손익금
        Double spotProfitRate, // 현물 수익률

        Long leverageBuyAmount, // 레버리지 매수(전체 포지션 크기, purchaseAmount)
        Long leverageEquityAmount, // 레버리지에 실제 투입된 내 돈(개시증거금)
        Long leverageProfitAmount, // 레버리지 손익금
        Double leverageProfitRate // 레버리지 수익률
) {
}
