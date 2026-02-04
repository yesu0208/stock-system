package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;

public class InMemorySingleStockAutoOrderQueue implements SingleStockAutoOrderQueue {

    private final PriorityBlockingQueue<AutoOrderDto> buyQueue;
    private final PriorityBlockingQueue<AutoOrderDto> sellQueue;

    private static final Comparator<AutoOrderDto> BUY_ORDER =
            Comparator
                    .comparing(AutoOrderDto::orderPrice)
                    .thenComparing(AutoOrderDto::orderTime);

    private static final Comparator<AutoOrderDto> SELL_ORDER =
            Comparator
                    .comparing(AutoOrderDto::orderPrice).reversed()
                    .thenComparing(AutoOrderDto::orderTime);

    public InMemorySingleStockAutoOrderQueue() {
        this.buyQueue = new PriorityBlockingQueue<>(11, BUY_ORDER);
        this.sellQueue = new PriorityBlockingQueue<>(11, SELL_ORDER);
    }

    @Override
    public void autoOrderEnqueue(AutoOrderDto autoOrderDto) {
        if (autoOrderDto.autoOrderType() == AutoOrderType.BUY) {
            buyQueue.offer(autoOrderDto);
        } else {
            sellQueue.offer(autoOrderDto);
        }

        System.out.println("=== PriorityBlockingQueue DEBUGGING(AUTO)===");
        // Todo: debugging(buyQueue)
        AutoOrderDto[] snapshot = buyQueue.toArray(new AutoOrderDto[0]);
        Arrays.sort(snapshot, BUY_ORDER);
        for (AutoOrderDto t : snapshot) {
            System.out.println(t);
        }

        // Todo: debugging(sellQueue)
        AutoOrderDto[] snapshot2 = sellQueue.toArray(new AutoOrderDto[0]);
        Arrays.sort(snapshot2, SELL_ORDER);
        for (AutoOrderDto t : snapshot2) {
            System.out.println(t);
        }

    }

    @Override
    public Optional<AutoOrderDto> peekBuy() {
        return Optional.ofNullable(buyQueue.peek());
    }

    @Override
    public Optional<AutoOrderDto> peekSell() {
        return Optional.ofNullable(sellQueue.peek());
    }

    @Override
    public AutoOrderDto pollBuy() {
        return buyQueue.poll();
    }

    @Override
    public AutoOrderDto pollSell() {
        return sellQueue.poll();
    }

    @Override
    public boolean removeByAutoOrderId(Long autoOrderId) {
        boolean removedBuy = buyQueue.removeIf(o -> o.autoOrderId().equals(autoOrderId));
        boolean removedSell = sellQueue.removeIf(o -> o.autoOrderId().equals(autoOrderId));
        return removedBuy || removedSell;
    }
}
