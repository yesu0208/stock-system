package arile.toy.stocksystem.bffserver.order.event;

public record QueuePositionEvent(
        Long orderId,
        String username,
        String stockCode,
        long quantityAhead
) {
}
