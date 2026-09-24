package arile.toy.stocksystem.bffserver.alert.service;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertFiredResponse;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class AlertFiredPushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerAlertResponseRepository bffServerAlertResponseRepository;

    @InjectMocks
    private AlertFiredPushService service;

    @Test
    @DisplayName("발동 알림(발동 가격·실제 현재가·발동 시각)을 먼저 보내고, 이어서 발동된 알림이 빠진 목록을 보낸다")
    void push() {
        Instant firedTime = Instant.parse("2026-09-24T01:15:00Z");
        List<AlertResponseMessage> remaining = List.of();
        given(bffServerAlertResponseRepository.findAll("user1")).willReturn(remaining);

        service.push(new AlertFiredEvent(1L, "user1", "005930", AlertDirection.ABOVE, 70_000, 70_300, firedTime));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/fired",
                new AlertFiredResponse(1L, "user1", "005930", AlertDirection.ABOVE, 70_000, 70_300, firedTime));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert", remaining);
    }
}
