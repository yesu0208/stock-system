package arile.toy.stocksystem.bffserver.alertcancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelErrorCode;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResultResponse;
import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AlertCancelResponsePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerAlertResponseRepository bffServerAlertResponseRepository;

    @InjectMocks
    private AlertCancelResponsePushService service;

    private static AlertCancelResponseEvent event(boolean success, AlertCancelErrorCode errorCode) {
        AlertCancelResponseEvent event = mock(AlertCancelResponseEvent.class);
        given(event.alertId()).willReturn(1L);
        given(event.username()).willReturn("user1");
        given(event.stockCode()).willReturn("005930");
        given(event.direction()).willReturn(AlertDirection.BELOW);
        given(event.triggerPrice()).willReturn(65_000);
        given(event.success()).willReturn(success);
        if (!success) {
            given(event.errorCode()).willReturn(errorCode);
        }
        return event;
    }

    @Test
    @DisplayName("성공: SUCCESS 결과를 먼저 보내고, 이어서 해제가 반영된 알림 목록을 보낸다")
    void success() {
        List<AlertResponseMessage> alerts = List.of(mock(AlertResponseMessage.class));
        given(bffServerAlertResponseRepository.findAll("user1")).willReturn(alerts);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/cancel",
                AlertCancelResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930",
                        AlertDirection.BELOW, 65_000, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert", alerts);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AlertCancelErrorCode.class)
    @DisplayName("실패: 알림 ID와 오류 안내 문구를 담은 ERROR 결과만 보내고, 목록은 조회하지 않는다")
    void failure(AlertCancelErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/cancel",
                AlertCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930",
                        AlertDirection.BELOW, 65_000, errorCode.userMessage()));
        verifyNoInteractions(bffServerAlertResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/cancel",
                AlertCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930",
                        AlertDirection.BELOW, 65_000, "알 수 없는 오류가 발생했습니다."));
    }
}
