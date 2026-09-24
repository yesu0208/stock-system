package arile.toy.stocksystem.bffserver.cancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResultResponse;
import arile.toy.stocksystem.bffserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
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
class CancelResponsePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerOrderResponseRepository bffServerOrderResponseRepository;

    @InjectMocks
    private CancelResponsePushService service;

    private static CancelResponseEvent event(boolean success, CancelErrorCode errorCode) {
        return CancelResponseEvent.of(1L, "user1", "005930", OrderType.SELL, 71_000, 5, success, errorCode);
    }

    @Test
    @DisplayName("성공: SUCCESS 결과를 먼저 보내고, 이어서 취소가 반영된 미체결 목록을 보낸다")
    void success() {
        List<OrderResponseMessage> pending = List.of(mock(OrderResponseMessage.class));
        given(bffServerOrderResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/cancel",
                CancelResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930", OrderType.SELL,
                        71_000, 5, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order", pending);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CancelErrorCode.class)
    @DisplayName("실패: 주문 ID와 오류 안내 문구를 담은 ERROR 결과만 보내고, 미체결 목록은 다시 조회하지 않는다")
    void failure(CancelErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/cancel",
                CancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930", OrderType.SELL,
                        71_000, 5, errorCode.userMessage()));
        verifyNoInteractions(bffServerOrderResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/cancel",
                CancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930", OrderType.SELL,
                        71_000, 5, "알 수 없는 오류가 발생했습니다."));
    }
}
