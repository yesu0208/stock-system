package arile.toy.stocksystem.stockserver.order.dto;

import java.util.Optional;

public interface SingleStockOrderQueue {
    void orderEnqueue(OrderDto orderDto);
    Optional<OrderDto> peekBuy();
    Optional<OrderDto> peekSell();
    OrderDto pollBuy();
    OrderDto pollSell();
    boolean removeByOrderId(Long orderId);
}
