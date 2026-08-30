package arile.toy.stocksystem.stockserver.autoorder.event.publisher;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;

public interface AutoOrderResponseEventPublisher {
    void publish(StockServerAutoOrderResponseMessage orderResponseMessage);
    void publishError(StockServerAutoOrderRequestEvent orderRequestEvent, AutoOrderResultCode resultCode);
    void publishTriggerFailure(AutoOrderDto autoOrderDto, AutoOrderResultCode resultCode); // 추가
    void publishTrigger(String username);
}
