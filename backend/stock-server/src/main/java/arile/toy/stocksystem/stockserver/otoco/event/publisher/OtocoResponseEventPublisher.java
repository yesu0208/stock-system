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
    void publishEntryFilled(OtocoDto dto);
    void publishEntryFailed(OtocoDto dto, OtocoResultCode resultCode);
    void publishExitTriggered(OtocoDto dto, OtocoLeg leg);
    void publishExitFailed(OtocoDto dto, OtocoResultCode resultCode);
}
