package arile.toy.stocksystem.bffserver.alertcancel.service;

import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelRequest;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResponse;
import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.bffserver.alertcancel.event.publisher.AlertCancelRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertCancelIngressService {

    private final AlertCancelRequestEventPublisher publisher;

    public AlertCancelResponse receive(AlertCancelRequest alertCancelRequest) {

        publisher.publishAlertCancel(AlertCancelRequestEvent.fromRequest(alertCancelRequest));

        return new AlertCancelResponse(alertCancelRequest.alertId(), alertCancelRequest.stockCode());
    }
}
