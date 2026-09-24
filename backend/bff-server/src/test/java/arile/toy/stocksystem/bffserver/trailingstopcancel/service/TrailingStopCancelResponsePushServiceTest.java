package arile.toy.stocksystem.bffserver.trailingstopcancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelResultResponse;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
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
class TrailingStopCancelResponsePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerTrailingStopResponseRepository bffServerTrailingStopResponseRepository;

    @InjectMocks
    private TrailingStopCancelResponsePushService service;

    private static TrailingStopCancelResponseEvent event(boolean success, TrailingStopCancelErrorCode errorCode) {
        return new TrailingStopCancelResponseEvent(1L, "user1", "005930", TrailingStopType.BUY,
                72_100, 5, success, errorCode);
    }

    @Test
    @DisplayName("성공: SUCCESS 결과를 먼저 보내고, 이어서 취소가 반영된 트레일링 스탑 목록을 보낸다")
    void success() {
        List<TrailingStopResponseMessage> pending = List.of(mock(TrailingStopResponseMessage.class));
        given(bffServerTrailingStopResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/cancel",
                TrailingStopCancelResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930",
                        TrailingStopType.BUY, 72_100, 5, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop", pending);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TrailingStopCancelErrorCode.class)
    @DisplayName("실패: 트레일링 스탑 ID와 오류 안내 문구를 담은 ERROR 결과만 보내고, 목록은 조회하지 않는다")
    void failure(TrailingStopCancelErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/cancel",
                TrailingStopCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930",
                        TrailingStopType.BUY, 72_100, 5, errorCode.userMessage()));
        verifyNoInteractions(bffServerTrailingStopResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/cancel",
                TrailingStopCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930",
                        TrailingStopType.BUY, 72_100, 5, "알 수 없는 오류가 발생했습니다."));
    }
}
