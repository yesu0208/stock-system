package arile.toy.stocksystem.stockserver.otoco.dto;

import lombok.Getter;

@Getter
public enum OtocoStatus {
    /** 진입 조건 대기 중 (아직 주문 미등록) */
    WAITING_ENTRY(true),
    /** 진입 주문이 큐에 등록되어 체결 대기 중 */
    ENTRY_ORDER_PLACED(true),
    /** 진입 체결 완료, TP/SL 감시 중 */
    WAITING_EXIT(true),
    /** TP 또는 SL 중 하나가 체결되어 완료 */
    COMPLETED(false),
    CANCELED(false);

    private final boolean open;

    OtocoStatus(boolean open) {
        this.open = open;
    }
}
