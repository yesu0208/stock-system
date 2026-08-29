package arile.toy.stocksystem.accountserver.useraccount.repository;

public interface AccountBalanceCommand {
    boolean reserveCash(String username, long amount);
    boolean refundReservedCash(String username, long amount);
    boolean reserveStock(String username, String stockCode, int quantity);
    boolean refundReservedStock(String username, String stockCode, int quantity);
}
