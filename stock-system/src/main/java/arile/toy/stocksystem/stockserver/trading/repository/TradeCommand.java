package arile.toy.stocksystem.stockserver.trading.repository;

public interface TradeCommand {
    boolean applyBuyTrade(String username, String stockCode, int quantity,
            long buyPrice, long tradeAmount, long differenceAmount);
    boolean applySellTrade(String username, String stockCode, int quantity,
            long buyPrice, long tradeAmount, long differenceAmount);
}
