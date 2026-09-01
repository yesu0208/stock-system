package arile.toy.stocksystem.bffserver.order.dto;

public record OrderResponse(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
}
