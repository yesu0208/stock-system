package arile.toy.stocksystem.bffserver.alert.service;

import arile.toy.stocksystem.bffserver.alert.dto.AlertFiredResponse;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertFiredPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerAlertResponseRepository bffServerAlertResponseRepository;

    public void push(AlertFiredEvent alertFiredEvent) {

        messagingTemplate.convertAndSendToUser(
                alertFiredEvent.username(),
                "/sub/alert/fired",
                new AlertFiredResponse(
                        alertFiredEvent.alertId(),
                        alertFiredEvent.username(),
                        alertFiredEvent.stockCode(),
                        alertFiredEvent.direction(),
                        alertFiredEvent.triggerPrice(),
                        alertFiredEvent.currentPrice(),
                        alertFiredEvent.firedTime()
                )
        );

        List<AlertResponseMessage> alerts
                = bffServerAlertResponseRepository.findAll(alertFiredEvent.username());

        messagingTemplate.convertAndSendToUser(
                alertFiredEvent.username(),
                "/sub/alert",
                alerts
        );
    }
}
