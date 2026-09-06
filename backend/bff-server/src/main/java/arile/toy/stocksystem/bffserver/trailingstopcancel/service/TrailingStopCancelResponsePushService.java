package arile.toy.stocksystem.bffserver.trailingstopcancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelResultResponse;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrailingStopCancelResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerTrailingStopResponseRepository bffServerTrailingStopResponseRepository;

    public void push(TrailingStopCancelResponseEvent event) {

        if (!event.success()) {

            String errorMessage =
                    event.errorCode() != null ? event.errorCode().userMessage() : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/trailing-stop/cancel",
                    TrailingStopCancelResultResponse.of(ResponseType.ERROR, event.trailingStopId(), event.username(),
                            event.stockCode(), event.trailingStopType(), event.triggerPrice(), event.orderQuantity(), errorMessage)
            );
        } else {

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/trailing-stop/cancel",
                    TrailingStopCancelResultResponse.of(ResponseType.SUCCESS, event.trailingStopId(), event.username(),
                            event.stockCode(), event.trailingStopType(), event.triggerPrice(), event.orderQuantity(), null)
            );

            List<TrailingStopResponseMessage> responses
                    = bffServerTrailingStopResponseRepository.findAll(event.username());

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/trailing-stop",
                    responses
            );
        }
    }
}
