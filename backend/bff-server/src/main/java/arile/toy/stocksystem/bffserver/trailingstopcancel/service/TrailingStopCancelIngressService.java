package arile.toy.stocksystem.bffserver.trailingstopcancel.service;

import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelRequest;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelResponse;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.publisher.TrailingStopCancelRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrailingStopCancelIngressService {

    private final TrailingStopCancelRequestEventPublisher publisher;

    public TrailingStopCancelResponse receive(TrailingStopCancelRequest request) {

        publisher.publishTrailingStopCancel(TrailingStopCancelRequestEvent.fromRequest(request));

        return new TrailingStopCancelResponse(request.trailingStopId(), request.stockCode());
    }
}
