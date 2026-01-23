package arile.toy.stocksystem.stockserver.external.stock.approvalkey;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApprovalKeyResponse(@JsonProperty("approval_key") String approvalKey) {
}
