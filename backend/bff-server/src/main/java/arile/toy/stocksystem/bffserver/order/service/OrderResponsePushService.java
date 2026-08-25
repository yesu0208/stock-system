package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.dto.OrderResultResponse;
import arile.toy.stocksystem.bffserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderResponsePushService {

    private final BffServerOrderResponseRepository bffServerOrderResponseRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void push(OrderResponseEvent orderResponseEvent) {

        if (!orderResponseEvent.success()) {

            String errorMessage =
                    orderResponseEvent.errorCode() != null
                            ? orderResponseEvent.errorCode().userMessage()
                            : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    orderResponseEvent.username(),
                    "/sub/order/result",
                    OrderResultResponse.of(ResponseType.ERROR, null, orderResponseEvent.username(),
                            orderResponseEvent.stockCode(), orderResponseEvent.orderType(),
                            orderResponseEvent.orderPrice(), orderResponseEvent.orderQuantity(), null, errorMessage

                    )
            );
        } else {

            List<OrderResponseMessage> responses
                    = bffServerOrderResponseRepository.findAll(orderResponseEvent.username());

            messagingTemplate.convertAndSendToUser(
                    orderResponseEvent.username(),
                    "/sub/order/result",
                    OrderResultResponse.of(ResponseType.SUCCESS, orderResponseEvent.orderId(), orderResponseEvent.username(),
                            orderResponseEvent.stockCode(), orderResponseEvent.orderType(),
                            orderResponseEvent.orderPrice(), orderResponseEvent.orderQuantity(), orderResponseEvent.orderTime(), null

                    )
            );

            messagingTemplate.convertAndSendToUser(
                    orderResponseEvent.username(),
                    "/sub/order",
                    responses
            );
        }
    }
}
