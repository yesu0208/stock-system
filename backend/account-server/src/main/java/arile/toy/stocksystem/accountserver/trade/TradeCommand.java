package arile.toy.stocksystem.accountserver.trade;

public interface TradeCommand {
    boolean applyBuyTrade(
            String username, String stockCode, int totalQuantity,
            long totalAmount, long totalCostAmount, long tradeAmount, long differenceAmount
    );

    boolean applySellTrade(
            String username, String stockCode, int totalQuantity,
            long totalAmount, long totalCostAmount, long tradeAmount, long differenceAmount
    );
}
