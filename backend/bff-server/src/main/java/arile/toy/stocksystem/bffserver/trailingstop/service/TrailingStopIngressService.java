package arile.toy.stocksystem.bffserver.trailingstop.service;

import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopRequest;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponse;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopRequestEvent;
import arile.toy.stocksystem.bffserver.trailingstop.event.publisher.TrailingStopRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrailingStopIngressService {

    private final TrailingStopRequestEventPublisher publisher;

    public TrailingStopResponse receive(String username, TrailingStopRequest request) {

        var leverageRatio = request.leverageRatioOrDefault();

        TrailingStopRequestEvent event = new TrailingStopRequestEvent(
                username,
                request.stockCode(),
                request.trailingStopType(),
                request.orderQuantity(),
                request.stopPercent(),
                request.basePrice(),
                leverageRatio
        );

        publisher.publishTrailingStop(event);

        return new TrailingStopResponse(username, request.stockCode(), request.trailingStopType(),
                request.orderQuantity(), request.stopPercent(), request.basePrice(), leverageRatio);
    }
}
