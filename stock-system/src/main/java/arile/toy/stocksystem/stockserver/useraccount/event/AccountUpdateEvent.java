package arile.toy.stocksystem.stockserver.useraccount.event;

public record AccountUpdateEvent(
        String username
) {
    public static AccountUpdateEvent of(String username){
        return new AccountUpdateEvent(username);
    }
}
