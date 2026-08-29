package arile.toy.stocksystem.accountserver.useraccount.dto;

import java.util.Set;

public record SettleAccountsRequest(
        Set<String> usernames
) {
}
