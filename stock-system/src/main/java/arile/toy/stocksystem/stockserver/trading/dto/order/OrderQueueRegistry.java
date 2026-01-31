package arile.toy.stocksystem.stockserver.trading.dto.order;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderQueueRegistry {
    private final ConcurrentHashMap<String, InMemorySingleStockOrderQueue> orderQueues = new ConcurrentHashMap<>();

    private InMemorySingleStockOrderQueue book(String stockCode) {
        return orderQueues.computeIfAbsent(stockCode, key -> new InMemorySingleStockOrderQueue() {
        });
    }

    public void orderEnqueue(OrderDto orderDto) {
        book(orderDto.stockCode()).orderEnqueue(orderDto);
    }

    public void orderCancel(Long orderId, String stockCode) {
        book(stockCode).removeByOrderId(orderId);
    }
    
    public Optional<OrderDto> peekBuy(String stockCode) {
        return book(stockCode).peekBuy();
    }

    public Optional<OrderDto> peekSell(String stockCode) {
        return book(stockCode).peekSell();
    }

    public OrderDto pollBuy(String stockCode) {
        return book(stockCode).pollBuy();
    }

    public OrderDto pollSell(String stockCode) {
        return book(stockCode).pollSell();
    }
}
