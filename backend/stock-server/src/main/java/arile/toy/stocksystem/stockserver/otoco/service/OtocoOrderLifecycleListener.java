package arile.toy.stocksystem.stockserver.otoco.service;

/**
 * 일반 주문(Order)의 체결/취소 시점에 그 주문이 OTOCO 진입 주문이었는지 확인하고
 * 후속 처리(TP/SL 활성화, 취소 반영)를 위임받기 위한 훅 인터페이스.
 *
 * TradeExecutionService(체결)와 CancelService(취소) 양쪽에서 호출되며,
 * OTOCO와 무관한 일반 주문일 경우 내부에서 조용히 no-op 처리됨.
 */
public interface OtocoOrderLifecycleListener {
    void onOrderFilled(Long orderId);
    void onOrderCanceled(Long orderId);
}
