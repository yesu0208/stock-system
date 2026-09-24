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

    public TrailingStopCancelResponse receive(String username, TrailingStopCancelRequest request) {

        publisher.publishTrailingStopCancel(TrailingStopCancelRequestEvent.fromRequest(username, request));

        return new TrailingStopCancelResponse(request.trailingStopId(), request.stockCode());
    }
}
