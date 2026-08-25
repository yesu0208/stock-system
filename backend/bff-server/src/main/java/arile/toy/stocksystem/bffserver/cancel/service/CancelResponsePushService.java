package arile.toy.stocksystem.bffserver.cancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResultResponse;
import arile.toy.stocksystem.bffserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CancelResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerOrderResponseRepository bffServerOrderResponseRepository;

    public void push(CancelResponseEvent cancelResponseEvent) {

        if (!cancelResponseEvent.success()) {

            String errorMessage =
                    cancelResponseEvent.errorCode() != null
                            ? cancelResponseEvent.errorCode().userMessage()
                            : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    cancelResponseEvent.username(),
                    "/sub/cancel",
                    CancelResultResponse.of(ResponseType.ERROR, cancelResponseEvent.orderId(), cancelResponseEvent.username(),
                            cancelResponseEvent.stockCode(), cancelResponseEvent.orderType(),
                            cancelResponseEvent.orderPrice(), cancelResponseEvent.orderQuantity(), errorMessage

                    )
            );
        } else {

            messagingTemplate.convertAndSendToUser(
                    cancelResponseEvent.username(),
                    "/sub/cancel",
                    CancelResultResponse.of(ResponseType.SUCCESS, cancelResponseEvent.orderId(), cancelResponseEvent.username(),
                            cancelResponseEvent.stockCode(), cancelResponseEvent.orderType(),
                            cancelResponseEvent.orderPrice(), cancelResponseEvent.orderQuantity(), null

                    )
            );

            List<OrderResponseMessage> responses
                    = bffServerOrderResponseRepository.findAll(cancelResponseEvent.username());

            messagingTemplate.convertAndSendToUser(
                    cancelResponseEvent.username(),
                    "/sub/order",
                    responses
            );
        }
    }
}
