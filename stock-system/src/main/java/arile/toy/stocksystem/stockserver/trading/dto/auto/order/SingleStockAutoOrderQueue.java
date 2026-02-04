package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import java.util.Optional;

public interface SingleStockAutoOrderQueue {
    void autoOrderEnqueue(AutoOrderDto autoOrderDto);
    Optional<AutoOrderDto> peekBuy();
    Optional<AutoOrderDto> peekSell();
    AutoOrderDto pollBuy();
    AutoOrderDto pollSell();
    boolean removeByAutoOrderId(Long orderId);
}
