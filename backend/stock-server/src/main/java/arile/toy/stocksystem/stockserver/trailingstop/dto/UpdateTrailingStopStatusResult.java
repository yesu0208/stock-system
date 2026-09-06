package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;

public record UpdateTrailingStopStatusResult(
        TrailingStopEntity trailingStopEntity,
        TrailingStopStatus previousStatus
) {
    public static UpdateTrailingStopStatusResult of(TrailingStopEntity trailingStopEntity, TrailingStopStatus previousStatus) {
        return new UpdateTrailingStopStatusResult(trailingStopEntity, previousStatus);
    }
}
