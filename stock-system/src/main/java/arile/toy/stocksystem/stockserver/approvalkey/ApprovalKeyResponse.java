package arile.toy.stocksystem.stockserver.approvalkey;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApprovalKeyResponse(@JsonProperty("approval_key") String approvalKey) {
}
