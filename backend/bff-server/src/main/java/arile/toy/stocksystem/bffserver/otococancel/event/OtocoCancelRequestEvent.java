package arile.toy.stocksystem.bffserver.otococancel.event;

import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelRequest;

public record OtocoCancelRequestEvent(
        Long otocoId,
        String stockCode,
        String username
) {
    public static OtocoCancelRequestEvent fromRequest(String username, OtocoCancelRequest request) {
        return new OtocoCancelRequestEvent(request.otocoId(), request.stockCode(), username);
    }
}
