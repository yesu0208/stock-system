package arile.toy.stocksystem.bffserver.cancel.service;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelRequest;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResponse;
import arile.toy.stocksystem.bffserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.bffserver.cancel.event.publisher.CancelRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelIngressService {

    private final CancelRequestEventPublisher publisher;

    public CancelResponse receive(CancelRequest cancelRequest) {
        
        publisher.publishCancel(CancelRequestEvent.fromRequest(cancelRequest));

        return new CancelResponse(cancelRequest.orderId(), cancelRequest.stockCode());
    }
}
