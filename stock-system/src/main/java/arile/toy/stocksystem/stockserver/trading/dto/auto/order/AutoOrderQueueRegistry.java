package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AutoOrderQueueRegistry {
    private final ConcurrentHashMap<String, InMemorySingleStockAutoOrderQueue> autoOrderQueues = new ConcurrentHashMap<>();

    private InMemorySingleStockAutoOrderQueue book(String stockCode) {
        return autoOrderQueues.computeIfAbsent(stockCode, key -> new InMemorySingleStockAutoOrderQueue() {
        });
    }

    public void autoOrderEnqueue(AutoOrderDto autoOrderDto) {
        book(autoOrderDto.stockCode()).autoOrderEnqueue(autoOrderDto);
    }

    public void autoOrderCancel(Long autoOrderId, String stockCode) {
        book(stockCode).removeByAutoOrderId(autoOrderId);
    }


    public Optional<AutoOrderDto> peekBuy(String stockCode) {
        return book(stockCode).peekBuy();
    }

    public Optional<AutoOrderDto> peekSell(String stockCode) {
        return book(stockCode).peekSell();
    }

    public AutoOrderDto pollBuy(String stockCode) {
        return book(stockCode).pollBuy();
    }

    public AutoOrderDto pollSell(String stockCode) {
        return book(stockCode).pollSell();
    }

}
