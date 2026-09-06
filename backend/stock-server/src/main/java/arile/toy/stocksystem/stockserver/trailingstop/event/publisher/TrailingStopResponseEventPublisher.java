package arile.toy.stocksystem.stockserver.trailingstop.event.publisher;

import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;

public interface TrailingStopResponseEventPublisher {
    void publish(StockServerTrailingStopResponseMessage message);
    void publishError(StockServerTrailingStopRequestEvent request, TrailingStopResultCode resultCode);
    void publishTrigger(String username);
    void publishTriggerFailure(TrailingStopDto dto, TrailingStopResultCode resultCode);
    void publishTrailingUpdate(TrailingStopDto dto);
}
