package arile.toy.stocksystem.accountserver.useraccount.repository;

public interface AccountBalanceCommand {
    boolean reserveCash(String username, long amount);
    boolean refundReservedCash(String username, long amount);
    boolean reserveStock(String username, String stockCode, int quantity);
    boolean refundReservedStock(String username, String stockCode, int quantity);

    /** 레버리지 매수 체결 정산: 예약분(reservedCash)에서 reservedDecrease만큼 해제하고, availableIncrease만큼 availableCash로 반환 */
    boolean settleLeverageBuy(String username, long reservedDecrease, long availableIncrease);

    /** 레버리지 매도 체결 대금을 availableCash에 직접 반영 (매도는 사전에 현금을 예약하지 않으므로 reservedCash는 건드리지 않음) */
    boolean creditAvailableCash(String username, long amount);

    /**
     * availableCash에서 amount만큼 무조건 차감한다 (잔액 부족 여부와 무관하게 음수로 내려갈 수 있음).
     * 신용이자처럼 "잔액이 부족해도 반드시 청구되어야 하는" 차감에 사용한다.
     * reserveCash와 달리 잔액 검증 후 실패 반환하는 방식이 아니다.
     */
    boolean debitAvailableCash(String username, long amount);
}
