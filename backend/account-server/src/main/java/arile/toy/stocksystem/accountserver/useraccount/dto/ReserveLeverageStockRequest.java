package arile.toy.stocksystem.accountserver.useraccount.dto;

public record ReserveLeverageStockRequest(
        String stockCode,
        String leverageRatio,
        int quantity
) {
}
