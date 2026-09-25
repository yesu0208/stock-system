package arile.toy.stocksystem.stockserver.order.dto;

/**
 * 주문·체결 금액 기준 레버리지 지표.
 * 주문 응답, 주문 내역, 체결 내역에서 같은 계산식을 쓰도록 한 곳에 모음.
 * 현물(또는 레버리지 미지정)은 명목금액만 계산하고 나머지 지표는 null.
 */
public record LeverageMetrics(
        long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice
) {
    // 유지증거율: 대출금 대비 담보 가치가 이 비율 아래로 떨어지면 청산
    private static final double MAINTENANCE_RATIO = 1.4;

    public static LeverageMetrics of(LeverageRatio leverageRatio, int price, int quantity) {
        long notionalValue = (long) price * quantity;

        if (leverageRatio == null || leverageRatio.isSpot()) {
            return new LeverageMetrics(notionalValue, null, null, null);
        }

        long margin = leverageRatio.calculateMarginDeposit(notionalValue);
        long loanAmount = leverageRatio.calculateLoanAmount(notionalValue);
        long liquidationPrice = quantity > 0
                ? Math.round((MAINTENANCE_RATIO * loanAmount) / quantity)
                : 0L;

        return new LeverageMetrics(notionalValue, margin, MAINTENANCE_RATIO, liquidationPrice);
    }
}
