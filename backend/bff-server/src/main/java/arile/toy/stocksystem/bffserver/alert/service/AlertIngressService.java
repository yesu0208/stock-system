package arile.toy.stocksystem.bffserver.alert.service;

import arile.toy.stocksystem.bffserver.alert.dto.AlertRequest;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponse;
import arile.toy.stocksystem.bffserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.bffserver.alert.event.publisher.AlertRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertIngressService {

    private final AlertRequestEventPublisher publisher;

    public AlertResponse receive(String username, AlertRequest request) {

        publisher.publishAlert(AlertRequestEvent.fromRequest(username, request));

        return new AlertResponse(username, request.stockCode(), request.direction(), request.triggerPrice());
    }
}
