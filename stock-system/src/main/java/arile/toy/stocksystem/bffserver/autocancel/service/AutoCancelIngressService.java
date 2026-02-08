package arile.toy.stocksystem.bffserver.autocancel.service;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResponse;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.autocancel.event.publisher.RedisAutoCancelRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoCancelIngressService {

    private final RedisAutoCancelRequestEventPublisher publisher;

    public AutoCancelResponse receive(AutoCancelRequest autoCancelRequest) {


        publisher.publishAutoCancel(AutoCancelRequestEvent.fromRequest(autoCancelRequest));

        return new AutoCancelResponse(autoCancelRequest.autoOrderId(), autoCancelRequest.stockCode());
    }
}
