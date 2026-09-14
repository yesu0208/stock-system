package arile.toy.stocksystem.bffserver.portfolio.dto;

public record PortfolioStockItem(
        String stockCode,
        /** 현물 보유분 평가금액 */
        Long spotAmount,
        /** 레버리지 보유분 평가금액(대출금 차감 없이 quantity × curPrice 그대로) */
        Long leverageAmount,
        /** spotAmount + leverageAmount */
        Long totalAmount,
        /** 이 종목이 속한 업종 내에서 차지하는 비중 (%) */
        Double ratioInSector,
        /** 전체 자산 대비 이 종목의 비중 (%) */
        Double ratioInTotal,
        /** 현물 + 레버리지 합산 손익금 */
        Long profitAmount,
        /** 손익 / (매입금액 + 개시증거금) 기준 수익률 (%) */
        Double profitRate
) {
}
