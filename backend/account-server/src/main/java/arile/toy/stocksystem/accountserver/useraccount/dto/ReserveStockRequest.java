package arile.toy.stocksystem.accountserver.useraccount.dto;

public record ReserveStockRequest(
        String stockCode,
        int quantity
) {
}
