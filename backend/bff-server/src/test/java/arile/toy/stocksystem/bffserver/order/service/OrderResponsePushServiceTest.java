package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.order.dto.OrderErrorCode;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.dto.OrderResultResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.OrderResponseEvent;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderResponsePushServiceTest {

    private static final Instant ORDER_TIME = Instant.parse("2026-09-24T00:30:00Z");

    @Mock private BffServerOrderResponseRepository bffServerOrderResponseRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderResponsePushService service;

    private static OrderResponseEvent event(boolean success, OrderErrorCode errorCode) {
        return new OrderResponseEvent(success ? 1L : null, "user1", "005930", OrderType.BUY, LeverageRatio.X2,
                70_000, 10, success ? ORDER_TIME : null, success, errorCode);
    }

    @Test
    @DisplayName("성공: 주문 ID·접수 시각을 담은 SUCCESS 결과를 먼저 보내고, 이어서 갱신된 미체결 목록을 보낸다")
    void success() {
        List<OrderResponseMessage> pending = List.of(mock(OrderResponseMessage.class));
        given(bffServerOrderResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order/result",
                OrderResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930", OrderType.BUY,
                        LeverageRatio.X2, 70_000, 10, ORDER_TIME, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order", pending);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OrderErrorCode.class)
    @DisplayName("실패: 오류 코드의 안내 문구를 담은 ERROR 결과만 보내고, 미체결 목록은 다시 조회하지 않는다")
    void failure(OrderErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order/result",
                OrderResultResponse.of(ResponseType.ERROR, null, "user1", "005930", OrderType.BUY,
                        LeverageRatio.X2, 70_000, 10, null, errorCode.userMessage()));
        verifyNoInteractions(bffServerOrderResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order/result",
                OrderResultResponse.of(ResponseType.ERROR, null, "user1", "005930", OrderType.BUY,
                        LeverageRatio.X2, 70_000, 10, null, "알 수 없는 오류가 발생했습니다."));
    }
}
