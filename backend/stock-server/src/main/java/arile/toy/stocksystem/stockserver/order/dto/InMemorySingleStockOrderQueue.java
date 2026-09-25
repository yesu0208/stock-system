package arile.toy.stocksystem.stockserver.order.dto;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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

    @Override
    public List<OrderDto> snapshotRanked(OrderType orderType) {
        PriorityBlockingQueue<OrderDto> queue = orderType == OrderType.BUY ? buyQueue : sellQueue;
        Comparator<OrderDto> comparator = orderType == OrderType.BUY ? BUY_ORDER : SELL_ORDER;

        OrderDto[] snapshot = queue.toArray(new OrderDto[0]);
        Arrays.sort(snapshot, comparator);
        return Arrays.asList(snapshot);
    }
}
