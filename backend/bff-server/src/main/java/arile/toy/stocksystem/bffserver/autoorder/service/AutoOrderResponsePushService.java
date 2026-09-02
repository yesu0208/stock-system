package arile.toy.stocksystem.bffserver.autoorder.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultResponse;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoOrderResponsePushService {

    private final BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void push(AutoOrderResponseEvent autoOrderResponseEvent) {

        if (!autoOrderResponseEvent.success()) {

            String errorMessage =
                    autoOrderResponseEvent.resultCode() != null
                            ? autoOrderResponseEvent.resultCode().userMessage()
                            : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    autoOrderResponseEvent.username(),
                    "/sub/auto/order/result",
                    AutoOrderResultResponse.of(ResponseType.ERROR, null, autoOrderResponseEvent.username(),
                            autoOrderResponseEvent.stockCode(), autoOrderResponseEvent.autoOrderType(), autoOrderResponseEvent.leverageRatio(),
                            autoOrderResponseEvent.triggerPrice(), autoOrderResponseEvent.orderPrice(), autoOrderResponseEvent.orderQuantity(),
                            null, errorMessage

                    )
            );
        } else if (autoOrderResponseEvent.resultCode() == AutoOrderResultCode.TRIGGERED) {

            List<AutoOrderResponseMessage> responses
                    = bffServerAutoOrderResponseRepository.findAll(autoOrderResponseEvent.username());

            messagingTemplate.convertAndSendToUser(
                    autoOrderResponseEvent.username(),
                    "/sub/auto/order",
                    responses
            );

        } else {

            List<AutoOrderResponseMessage> responses
                    = bffServerAutoOrderResponseRepository.findAll(autoOrderResponseEvent.username());

            messagingTemplate.convertAndSendToUser(
                    autoOrderResponseEvent.username(),
                    "/sub/auto/order/result",
                    AutoOrderResultResponse.of(ResponseType.SUCCESS, autoOrderResponseEvent.autoOrderId(), autoOrderResponseEvent.username(),
                            autoOrderResponseEvent.stockCode(), autoOrderResponseEvent.autoOrderType(), autoOrderResponseEvent.leverageRatio(),
                            autoOrderResponseEvent.triggerPrice(), autoOrderResponseEvent.orderPrice(), autoOrderResponseEvent.orderQuantity(),
                            autoOrderResponseEvent.orderTime(), null

                    )
            );

            messagingTemplate.convertAndSendToUser(
                    autoOrderResponseEvent.username(),
                    "/sub/auto/order",
                    responses
            );
        }
    }
}
