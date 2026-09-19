package arile.toy.stocksystem.stockserver.otoco.event.publisher;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoLeg;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;

public interface OtocoResponseEventPublisher {
    void publish(StockServerOtocoResponseMessage message);
    void publishError(StockServerOtocoRequestEvent request, OtocoResultCode resultCode);
    void publishEntryTriggered(OtocoDto dto);
    void publishEntryPartiallyFilled(OtocoDto dto);
    void publishEntryFilled(OtocoDto dto);
    void publishEntryFailed(OtocoDto dto, OtocoResultCode resultCode);
    // 진입 주문 자체가 사용자에 의해 정상 취소되어 OTOCO가 종료된 경우 — 오류가 아니라 정상 종료(success)로 발행
    void publishEntryCanceled(OtocoDto dto);
    void publishExitTriggered(OtocoDto dto, OtocoLeg leg);
    void publishExitFailed(OtocoDto dto, OtocoResultCode resultCode);
}
