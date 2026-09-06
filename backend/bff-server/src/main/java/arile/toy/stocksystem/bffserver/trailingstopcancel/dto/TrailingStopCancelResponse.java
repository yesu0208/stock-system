package arile.toy.stocksystem.bffserver.trailingstopcancel.dto;

public record TrailingStopCancelResponse(
        Long trailingStopId,
        String stockCode
) {
}
