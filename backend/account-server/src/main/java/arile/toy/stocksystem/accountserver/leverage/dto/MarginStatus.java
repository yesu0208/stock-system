package arile.toy.stocksystem.accountserver.leverage.dto;

public enum MarginStatus {
    /** 정상 (담보비율 140% 이상) */
    NORMAL,
    /** D일: 마진콜 발생, D+1일 유예 중 */
    MARGIN_CALL,
    /** D+1일 유예 종료 후에도 미해결, 다음 배치에서 반대매매 대상 */
    LIQUIDATION_PENDING
}
