package arile.toy.stocksystem.accountserver.useraccount.dto;

public record RefundStockRequest(
        String stockCode,
        int quantity) {
}
