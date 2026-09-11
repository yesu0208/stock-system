package arile.toy.stocksystem.bffserver.otococancel.service;

import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelRequest;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelResponse;
import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.otococancel.event.publisher.OtocoCancelRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtocoCancelIngressService {

    private final OtocoCancelRequestEventPublisher publisher;

    public OtocoCancelResponse receive(OtocoCancelRequest otocoCancelRequest) {

        publisher.publishOtocoCancel(OtocoCancelRequestEvent.fromRequest(otocoCancelRequest));

        return new OtocoCancelResponse(otocoCancelRequest.otocoId(), otocoCancelRequest.stockCode());
    }
}
