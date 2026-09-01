package arile.toy.stocksystem.accountserver.useraccount.dto;

public enum AccountStatus {
    /** 정상 */
    NORMAL,
    /** 반대매매 후 부족분 발생, 3영업일 유예 중 (매도만 허용) */
    NEGATIVE,
    /** 유예 기간 내 미해소 -> 영구 정지 (거래 전면 금지) */
    SUSPENDED
}
