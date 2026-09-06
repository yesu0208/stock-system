package arile.toy.stocksystem.bffserver.otoco.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResultResponse;
import arile.toy.stocksystem.bffserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OtocoResponsePushService {

    private static final Set<OtocoResultCode> LIST_REFRESH_CODES = Set.of(
            OtocoResultCode.ENTRY_TRIGGERED, OtocoResultCode.ENTRY_FILLED,
            OtocoResultCode.TP_TRIGGERED, OtocoResultCode.SL_TRIGGERED
    );

    private final BffServerOtocoResponseRepository bffServerOtocoResponseRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void push(OtocoResponseEvent event) {

        if (!event.success()) {

            String errorMessage =
                    event.resultCode() != null ? event.resultCode().userMessage() : "알 수 없는 오류가 발생했습니다.";

            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/otoco/result",
                    OtocoResultResponse.of(ResponseType.ERROR, event.otocoId(), event.username(), event.stockCode(),
                            event.entryDirection(), event.leverageRatio(), event.orderQuantity(),
                            event.entryTriggerPrice(), event.tpTriggerPrice(), event.slTriggerPrice(),
                            event.otocoStatus(), event.orderTime(), errorMessage)
            );
            return;
        }

        if (event.resultCode() != null && LIST_REFRESH_CODES.contains(event.resultCode())) {

            // 단계 전환 알림(진입발동/진입체결/TP·SL체결)은 결과 메시지 + 목록 갱신을 함께 보냄.
            messagingTemplate.convertAndSendToUser(
                    event.username(),
                    "/sub/otoco/result",
                    OtocoResultResponse.of(ResponseType.SUCCESS, event.otocoId(), event.username(), event.stockCode(),
                            event.entryDirection(), event.leverageRatio(), event.orderQuantity(),
                            event.entryTriggerPrice(), event.tpTriggerPrice(), event.slTriggerPrice(),
                            event.otocoStatus(), event.orderTime(), null)
            );

            List<OtocoResponseMessage> responses = bffServerOtocoResponseRepository.findAll(event.username());
            messagingTemplate.convertAndSendToUser(event.username(), "/sub/otoco", responses);
            return;
        }

        // 신규 등록 성공
        messagingTemplate.convertAndSendToUser(
                event.username(),
                "/sub/otoco/result",
                OtocoResultResponse.of(ResponseType.SUCCESS, event.otocoId(), event.username(), event.stockCode(),
                        event.entryDirection(), event.leverageRatio(), event.orderQuantity(),
                        event.entryTriggerPrice(), event.tpTriggerPrice(), event.slTriggerPrice(),
                        event.otocoStatus(), event.orderTime(), null)
        );

        List<OtocoResponseMessage> responses = bffServerOtocoResponseRepository.findAll(event.username());
        messagingTemplate.convertAndSendToUser(event.username(), "/sub/otoco", responses);
    }
}
