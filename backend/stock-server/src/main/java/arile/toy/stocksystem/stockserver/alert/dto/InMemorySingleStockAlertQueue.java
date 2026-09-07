package arile.toy.stocksystem.stockserver.alert.dto;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;

public class InMemorySingleStockAlertQueue implements SingleStockAlertQueue {

    private final PriorityBlockingQueue<AlertDto> aboveQueue;
    private final PriorityBlockingQueue<AlertDto> belowQueue;

    private static final Comparator<AlertDto> ABOVE_ORDER =
            Comparator
                    .comparing(AlertDto::triggerPrice)
                    .thenComparing(AlertDto::registeredTime);

    private static final Comparator<AlertDto> BELOW_ORDER =
            Comparator
                    .comparing(AlertDto::triggerPrice).reversed()
                    .thenComparing(AlertDto::registeredTime);

    public InMemorySingleStockAlertQueue() {
        this.aboveQueue = new PriorityBlockingQueue<>(11, ABOVE_ORDER);
        this.belowQueue = new PriorityBlockingQueue<>(11, BELOW_ORDER);
    }

    @Override
    public void alertEnqueue(AlertDto alertDto) {
        if (alertDto.direction() == AlertDirection.ABOVE) {
            aboveQueue.offer(alertDto);
        } else {
            belowQueue.offer(alertDto);
        }
    }

    @Override
    public Optional<AlertDto> peekAbove() {
        return Optional.ofNullable(aboveQueue.peek());
    }

    @Override
    public Optional<AlertDto> peekBelow() {
        return Optional.ofNullable(belowQueue.peek());
    }

    @Override
    public AlertDto pollAbove() {
        return aboveQueue.poll();
    }

    @Override
    public AlertDto pollBelow() {
        return belowQueue.poll();
    }

    @Override
    public boolean removeByAlertId(Long alertId) {
        boolean removedAbove = aboveQueue.removeIf(a -> a.alertId().equals(alertId));
        boolean removedBelow = belowQueue.removeIf(a -> a.alertId().equals(alertId));
        return removedAbove || removedBelow;
    }
}
