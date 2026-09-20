package arile.toy.stocksystem.stockserver.order.event;

public record QueuePositionEvent(
        Long orderId,
        String username,
        String stockCode,
        long quantityAhead
) {
    public static QueuePositionEvent of(Long orderId, String username, String stockCode, long quantityAhead) {
        return new QueuePositionEvent(orderId, username, stockCode, quantityAhead);
    }
}
