package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(
        @NotNull ReactionType reactionType
) {
}
