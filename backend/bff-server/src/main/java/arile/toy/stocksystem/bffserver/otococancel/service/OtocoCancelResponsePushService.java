package arile.toy.stocksystem.bffserver.otococancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelResultResponse;
import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OtocoCancelResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerOtocoResponseRepository bffServerOtocoResponseRepository;

    public void push(OtocoCancelResponseEvent event) {

        if (!event.success()) {

            String errorMessage =
                    event.errorCode() != null ? event.errorCode().userMessage() : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/otoco/cancel",
                    OtocoCancelResultResponse.of(ResponseType.ERROR, event.otocoId(), event.username(),
                            event.stockCode(), event.entryDirection(), event.entryTriggerPrice(),
                            event.orderQuantity(), errorMessage)
            );
        } else {

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/otoco/cancel",
                    OtocoCancelResultResponse.of(ResponseType.SUCCESS, event.otocoId(), event.username(),
                            event.stockCode(), event.entryDirection(), event.entryTriggerPrice(),
                            event.orderQuantity(), null)
            );

            List<OtocoResponseMessage> responses = bffServerOtocoResponseRepository.findAll(event.username());
            messagingTemplate.convertAndSendToUser(event.username(), "/sub/otoco", responses);
        }
    }
}
