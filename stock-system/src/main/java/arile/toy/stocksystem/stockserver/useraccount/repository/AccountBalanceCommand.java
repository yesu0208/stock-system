package arile.toy.stocksystem.stockserver.useraccount.repository;

public interface AccountBalanceCommand {
    boolean reserveCash(String username, long amount);
    boolean refundReservedCash(String username, long amount);
}

