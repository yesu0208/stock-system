package arile.toy.stocksystem.bffserver.alertcancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResultResponse;
import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertCancelResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerAlertResponseRepository bffServerAlertResponseRepository;

    public void push(AlertCancelResponseEvent alertCancelResponseEvent) {

        if (!alertCancelResponseEvent.success()) {

            String errorMessage =
                    alertCancelResponseEvent.errorCode() != null
                            ? alertCancelResponseEvent.errorCode().userMessage()
                            : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    alertCancelResponseEvent.username(),
                    "/sub/alert/cancel",
                    AlertCancelResultResponse.of(ResponseType.ERROR, alertCancelResponseEvent.alertId(), alertCancelResponseEvent.username(),
                            alertCancelResponseEvent.stockCode(), alertCancelResponseEvent.direction(),
                            alertCancelResponseEvent.triggerPrice(), errorMessage
                    )
            );
            return;
        }

        messagingTemplate.convertAndSendToUser(
                alertCancelResponseEvent.username(),
                "/sub/alert/cancel",
                AlertCancelResultResponse.of(ResponseType.SUCCESS, alertCancelResponseEvent.alertId(), alertCancelResponseEvent.username(),
                        alertCancelResponseEvent.stockCode(), alertCancelResponseEvent.direction(),
                        alertCancelResponseEvent.triggerPrice(), null
                )
        );

        List<AlertResponseMessage> alerts
                = bffServerAlertResponseRepository.findAll(alertCancelResponseEvent.username());

        messagingTemplate.convertAndSendToUser(
                alertCancelResponseEvent.username(),
                "/sub/alert",
                alerts
        );
    }
}
