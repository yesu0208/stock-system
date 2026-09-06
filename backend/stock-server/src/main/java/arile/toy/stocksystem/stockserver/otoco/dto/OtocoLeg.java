package arile.toy.stocksystem.stockserver.otoco.dto;

/** OCO 청산 완료 시 어느 쪽 조건이 체결됐는지 구분 */
public enum OtocoLeg {
    TAKE_PROFIT,
    STOP_LOSS
}
