package arile.toy.stocksystem.accountserver.useraccount.dto;

public record BalanceCommandResponse(
        boolean success
) {
    public static BalanceCommandResponse of(boolean success) {
        return new BalanceCommandResponse(success);
    }
}
