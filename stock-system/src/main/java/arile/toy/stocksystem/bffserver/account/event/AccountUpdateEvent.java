package arile.toy.stocksystem.bffserver.account.event;

public record AccountUpdateEvent(
        String username
) {
    public static AccountUpdateEvent of(String username){
        return new AccountUpdateEvent(username);
    }
}
