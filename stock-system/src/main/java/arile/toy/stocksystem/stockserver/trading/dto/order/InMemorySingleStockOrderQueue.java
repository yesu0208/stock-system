package arile.toy.stocksystem.stockserver.trading.dto.order;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;

public class InMemorySingleStockOrderQueue implements SingleStockOrderQueue {

    private final PriorityBlockingQueue<OrderDto> buyQueue;
    private final PriorityBlockingQueue<OrderDto> sellQueue;

    private static final Comparator<OrderDto> BUY_ORDER =
            Comparator
                    .comparing(OrderDto::orderPrice).reversed()
                    .thenComparing(OrderDto::orderTime);

    private static final Comparator<OrderDto> SELL_ORDER =
            Comparator
                    .comparing(OrderDto::orderPrice)
                    .thenComparing(OrderDto::orderTime);

    public InMemorySingleStockOrderQueue() {
        this.buyQueue = new PriorityBlockingQueue<>(11, BUY_ORDER);
        this.sellQueue = new PriorityBlockingQueue<>(11, SELL_ORDER);
    }

    @Override
    public void orderEnqueue(OrderDto orderDto) {
        if (orderDto.orderType() == OrderType.BUY) {
            buyQueue.offer(orderDto);
        } else {
            sellQueue.offer(orderDto);
        }

        System.out.println("=== PriorityBlockingQueue DEBUGGING===");
        OrderDto[] snapshot = buyQueue.toArray(new OrderDto[0]);
        Arrays.sort(snapshot, BUY_ORDER);
        for (OrderDto t : snapshot) {
            System.out.println(t);
        }

        OrderDto[] snapshot2 = sellQueue.toArray(new OrderDto[0]);
        Arrays.sort(snapshot2, SELL_ORDER);
        for (OrderDto t : snapshot2) {
            System.out.println(t);
        }

    }

    @Override
    public Optional<OrderDto> peekBuy() {
        return Optional.ofNullable(buyQueue.peek());
    }

    @Override
    public Optional<OrderDto> peekSell() {
        return Optional.ofNullable(sellQueue.peek());
    }

    @Override
    public OrderDto pollBuy() {
        return buyQueue.poll();
    }

    @Override
    public OrderDto pollSell() {
        return sellQueue.poll();
    }

    @Override
    public boolean removeByOrderId(Long orderId) {
        boolean removedBuy = buyQueue.removeIf(o -> o.orderId().equals(orderId));
        boolean removedSell = sellQueue.removeIf(o -> o.orderId().equals(orderId));
        return removedBuy || removedSell;
    }
}
