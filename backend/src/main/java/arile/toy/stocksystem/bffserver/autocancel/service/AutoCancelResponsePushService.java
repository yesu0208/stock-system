package arile.toy.stocksystem.bffserver.autocancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResultResponse;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoCancelResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;

    public void push(AutoCancelResponseEvent autoCancelResponseEvent) {

        if (!autoCancelResponseEvent.success()) {

            String errorMessage =
                    autoCancelResponseEvent.errorCode() != null
                            ? autoCancelResponseEvent.errorCode().userMessage()
                            : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    autoCancelResponseEvent.username(),
                    "/sub/auto/cancel",
                    AutoCancelResultResponse.of(ResponseType.ERROR, autoCancelResponseEvent.autoOrderId(), autoCancelResponseEvent.username(),
                            autoCancelResponseEvent.stockCode(), autoCancelResponseEvent.autoOrderType(), autoCancelResponseEvent.triggerPrice(),
                            autoCancelResponseEvent.orderPrice(), autoCancelResponseEvent.orderQuantity(), errorMessage

                    )
            );
        } else {

            messagingTemplate.convertAndSendToUser(
                    autoCancelResponseEvent.username(),
                    "/sub/auto/cancel",
                    AutoCancelResultResponse.of(ResponseType.SUCCESS, autoCancelResponseEvent.autoOrderId(), autoCancelResponseEvent.username(),
                            autoCancelResponseEvent.stockCode(), autoCancelResponseEvent.autoOrderType(), autoCancelResponseEvent.triggerPrice(),
                            autoCancelResponseEvent.orderPrice(), autoCancelResponseEvent.orderQuantity(), null

                    )
            );

            List<AutoOrderResponseMessage> responses
                    = bffServerAutoOrderResponseRepository.findAll(autoCancelResponseEvent.username());

            messagingTemplate.convertAndSendToUser(
                    autoCancelResponseEvent.username(),
                    "/sub/auto/order",
                    responses
            );
        }
    }
}
