package arile.toy.stocksystem.stockserver.alert.dto;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertQueueRegistry {
    private final ConcurrentHashMap<String, InMemorySingleStockAlertQueue> alertQueues = new ConcurrentHashMap<>();

    private InMemorySingleStockAlertQueue book(String stockCode) {
        return alertQueues.computeIfAbsent(stockCode, key -> new InMemorySingleStockAlertQueue());
    }

    public void alertEnqueue(AlertDto alertDto) {
        book(alertDto.stockCode()).alertEnqueue(alertDto);
    }

    public void alertCancel(Long alertId, String stockCode) {
        book(stockCode).removeByAlertId(alertId);
    }

    public Optional<AlertDto> peekAbove(String stockCode) {
        return book(stockCode).peekAbove();
    }

    public Optional<AlertDto> peekBelow(String stockCode) {
        return book(stockCode).peekBelow();
    }

    public AlertDto pollAbove(String stockCode) {
        return book(stockCode).pollAbove();
    }

    public AlertDto pollBelow(String stockCode) {
        return book(stockCode).pollBelow();
    }
}
