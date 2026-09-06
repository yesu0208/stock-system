package arile.toy.stocksystem.bffserver.trailingstop.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResultResponse;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopResponseEvent;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrailingStopResponsePushService {

    private final BffServerTrailingStopResponseRepository bffServerTrailingStopResponseRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void push(TrailingStopResponseEvent event) {

        if (!event.success()) {

            String errorMessage =
                    event.resultCode() != null ? event.resultCode().userMessage() : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/trailing-stop/result",
                    TrailingStopResultResponse.of(ResponseType.ERROR, event.trailingStopId(), event.username(),
                            event.stockCode(), event.trailingStopType(), event.leverageRatio(), event.orderQuantity(),
                            event.stopPercent(), event.basePrice(), event.triggerPrice(), event.orderTime(), errorMessage)
            );
            return;
        }

        if (event.resultCode() == TrailingStopResultCode.TRIGGERED) {

            List<TrailingStopResponseMessage> responses
                    = bffServerTrailingStopResponseRepository.findAll(event.username());

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/trailing-stop",
                    responses
            );
            return;
        }

        if (event.resultCode() == TrailingStopResultCode.TRAILING_UPDATED) {

            TrailingStopResponseMessage updated = new TrailingStopResponseMessage(
                    event.trailingStopId(), event.username(), event.stockCode(), event.trailingStopType(),
                    event.leverageRatio(), event.orderQuantity(), event.stopPercent(), event.basePrice(),
                    event.triggerPrice(), event.orderTime()
            );

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/trailing-stop/update",
                    updated
            );
            return;
        }

        messagingTemplate.convertAndSendToUser(
                event.username(),
                "/sub/trailing-stop/result",
                TrailingStopResultResponse.of(ResponseType.SUCCESS, event.trailingStopId(), event.username(),
                        event.stockCode(), event.trailingStopType(), event.leverageRatio(), event.orderQuantity(),
                        event.stopPercent(), event.basePrice(), event.triggerPrice(), event.orderTime(), null)
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
