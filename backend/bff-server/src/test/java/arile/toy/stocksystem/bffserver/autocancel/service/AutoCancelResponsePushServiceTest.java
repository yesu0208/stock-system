package arile.toy.stocksystem.bffserver.autocancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResultResponse;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
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
class AutoCancelResponsePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;

    @InjectMocks
    private AutoCancelResponsePushService service;

    private static AutoCancelResponseEvent event(boolean success, AutoCancelErrorCode errorCode) {
        return AutoCancelResponseEvent.of(1L, "user1", "005930", AutoOrderType.SELL,
                72_000, 71_500, 5, success, errorCode);
    }

    @Test
    @DisplayName("성공: SUCCESS 결과를 먼저 보내고, 이어서 취소가 반영된 자동 주문 목록을 보낸다")
    void success() {
        List<AutoOrderResponseMessage> pending = List.of(mock(AutoOrderResponseMessage.class));
        given(bffServerAutoOrderResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/cancel",
                AutoCancelResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930", AutoOrderType.SELL,
                        72_000, 71_500, 5, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/order", pending);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AutoCancelErrorCode.class)
    @DisplayName("실패: 자동 주문 ID와 오류 안내 문구를 담은 ERROR 결과만 보내고, 목록은 다시 조회하지 않는다")
    void failure(AutoCancelErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/cancel",
                AutoCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930", AutoOrderType.SELL,
                        72_000, 71_500, 5, errorCode.userMessage()));
        verifyNoInteractions(bffServerAutoOrderResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/cancel",
                AutoCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930", AutoOrderType.SELL,
                        72_000, 71_500, 5, "알 수 없는 오류가 발생했습니다."));
    }
}
