package arile.toy.stocksystem.bffserver.alert.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResultResponse;
import arile.toy.stocksystem.bffserver.alert.event.AlertResponseEvent;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerAlertResponseRepository bffServerAlertResponseRepository;

    public void push(AlertResponseEvent alertResponseEvent) {

        if (!alertResponseEvent.success()) {

            String errorMessage =
                    alertResponseEvent.errorCode() != null
                            ? alertResponseEvent.errorCode().userMessage()
                            : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    alertResponseEvent.username(),
                    "/sub/alert/result",
                    AlertResultResponse.of(ResponseType.ERROR, null, alertResponseEvent.username(),
                            alertResponseEvent.stockCode(), alertResponseEvent.direction(), alertResponseEvent.triggerPrice(),
                            null, errorMessage
                    )
            );
            return;
        }

        messagingTemplate.convertAndSendToUser(
                alertResponseEvent.username(),
                "/sub/alert/result",
                AlertResultResponse.of(ResponseType.SUCCESS, alertResponseEvent.alertId(), alertResponseEvent.username(),
                        alertResponseEvent.stockCode(), alertResponseEvent.direction(), alertResponseEvent.triggerPrice(),
                        alertResponseEvent.registeredTime(), null
                )
        );

        List<AlertResponseMessage> alerts
                = bffServerAlertResponseRepository.findAll(alertResponseEvent.username());

        messagingTemplate.convertAndSendToUser(
                alertResponseEvent.username(),
                "/sub/alert",
                alerts
        );
    }
}
