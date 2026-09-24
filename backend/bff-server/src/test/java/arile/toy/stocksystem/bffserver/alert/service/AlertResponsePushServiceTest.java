package arile.toy.stocksystem.bffserver.alert.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertErrorCode;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResultResponse;
import arile.toy.stocksystem.bffserver.alert.event.AlertResponseEvent;
import arile.toy.stocksystem.bffserver.alert.repository.BffServerAlertResponseRepository;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AlertResponsePushServiceTest {

    private static final Instant REGISTERED_TIME = Instant.parse("2026-09-24T00:30:00Z");

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerAlertResponseRepository bffServerAlertResponseRepository;

    @InjectMocks
    private AlertResponsePushService service;

    private static AlertResponseEvent event(boolean success, AlertErrorCode errorCode) {
        return new AlertResponseEvent(success ? 1L : null, "user1", "005930", AlertDirection.ABOVE,
                70_000, success ? REGISTERED_TIME : null, success, errorCode);
    }

    @Test
    @DisplayName("성공: 알림 ID·등록 시각을 담은 SUCCESS 결과를 먼저 보내고, 이어서 알림 목록을 보낸다")
    void success() {
        List<AlertResponseMessage> alerts = List.of(mock(AlertResponseMessage.class));
        given(bffServerAlertResponseRepository.findAll("user1")).willReturn(alerts);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/result",
                AlertResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930", AlertDirection.ABOVE,
                        70_000, REGISTERED_TIME, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert", alerts);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AlertErrorCode.class)
    @DisplayName("실패: 오류 안내 문구를 담은 ERROR 결과만 보내고, 목록은 조회하지 않는다")
    void failure(AlertErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/result",
                AlertResultResponse.of(ResponseType.ERROR, null, "user1", "005930", AlertDirection.ABOVE,
                        70_000, null, errorCode.userMessage()));
        verifyNoInteractions(bffServerAlertResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/alert/result",
                AlertResultResponse.of(ResponseType.ERROR, null, "user1", "005930", AlertDirection.ABOVE,
                        70_000, null, "알 수 없는 오류가 발생했습니다."));
    }
}
