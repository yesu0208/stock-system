package arile.toy.stocksystem.bffserver.otoco.service;

import arile.toy.stocksystem.bffserver.otoco.dto.OtocoRequest;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponse;
import arile.toy.stocksystem.bffserver.otoco.event.OtocoRequestEvent;
import arile.toy.stocksystem.bffserver.otoco.event.publisher.OtocoRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtocoIngressService {

    private final OtocoRequestEventPublisher publisher;

    public OtocoResponse receive(String username, OtocoRequest request) {

        var leverageRatio = request.leverageRatioOrDefault();

        OtocoRequestEvent event = new OtocoRequestEvent(
                username, request.stockCode(), request.entryDirection(), request.orderQuantity(),
                request.entryTriggerPrice(), request.tpMode(), request.tpPrice(), request.tpPct(),
                request.slMode(), request.slPrice(), request.slPct(), leverageRatio
        );

        publisher.publishOtoco(event);

        return new OtocoResponse(username, request.stockCode(), request.entryDirection(), request.orderQuantity(),
                request.entryTriggerPrice(), request.tpMode(), request.tpPrice(), request.tpPct(),
                request.slMode(), request.slPrice(), request.slPct(), leverageRatio);
    }
}
