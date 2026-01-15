package arile.toy.stocksystem.websocket.approvalkey;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApprovalKeyResponse(@JsonProperty("approval_key") String approvalKey) {}
