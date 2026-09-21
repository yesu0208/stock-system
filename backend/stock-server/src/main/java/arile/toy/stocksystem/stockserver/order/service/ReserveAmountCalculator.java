package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.springframework.stereotype.Component;

/**
 * 매수 시 예약해야 할 현금(reserveCash로 넘길 금액)을 계산
 * 기존에 OrderService/AutoOrderService/TrailingStopService/OtocoService
 * 4곳에 거의 동일하게 중복돼 있던 로직을 하나로 모음.
 *
 * 레버리지면 매수금액 전체가 아니라 개시증거금만 예약하고,
 * 여기에 위탁수수료(주문가 기준)까지 포함해서 예약해야
 * "가진 현금 그대로 최대수량 매수" 시 체결 후 수수료 때문에
 * 마이너스로 떨어지는 걸 막을 수 있음.
 * (거래세는 매도에만 붙으므로 매수 예약엔 포함하지 x)
 */
@Component
public class ReserveAmountCalculator {

    private static final double FEE_RATE = 0.00015;

    public long calculateFee(long amount) {
        if (amount <= 0) {
            return 0L;
        }
        return Math.round(amount * FEE_RATE);
    }

    /**
     * @param orderAmount 주문가 × 수량 (레버리지 전체 매수금액)
     */
    public long calculateReserveAmount(LeverageRatio leverageRatio, long orderAmount) {
        long principal = leverageRatio.isSpot() ? orderAmount : leverageRatio.calculateMarginDeposit(orderAmount);
        long fee = calculateFee(orderAmount);
        return principal + fee;
    }
}
