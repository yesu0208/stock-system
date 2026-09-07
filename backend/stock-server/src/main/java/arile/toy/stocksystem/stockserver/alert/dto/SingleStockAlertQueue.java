package arile.toy.stocksystem.stockserver.alert.dto;

import java.util.Optional;

public interface SingleStockAlertQueue {
    void alertEnqueue(AlertDto alertDto);
    Optional<AlertDto> peekAbove();
    Optional<AlertDto> peekBelow();
    AlertDto pollAbove();
    AlertDto pollBelow();
    boolean removeByAlertId(Long alertId);
}
