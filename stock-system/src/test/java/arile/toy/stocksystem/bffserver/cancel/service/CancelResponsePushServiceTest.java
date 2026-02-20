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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelResponsePushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BffServerOrderResponseRepository repository;

    @InjectMocks
    private CancelResponsePushService service;

    @Test
    @DisplayName("실패 이벤트 처리 시 에러 메시지 전송")
    void givenFailedEvent_whenPush_thenSendsErrorMessage() {
        // given
        CancelResponseEvent event = new CancelResponseEvent(
                1L, "user1", "005930",
                OrderType.BUY, 50000, 50, false, CancelErrorCode.INTERNAL_ERROR
        );

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/cancel"),
                argThat(resp -> resp instanceof CancelResultResponse
                        && ((CancelResultResponse) resp).responseType() == ResponseType.ERROR
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("성공 이벤트 처리 시 성공 메시지와 주문 리스트 전송")
    void givenSuccessEvent_whenPush_thenSendsSuccessMessageAndOrderList() {
        // given
        CancelResponseEvent event = new CancelResponseEvent(
                1L, "user1", "005930",
                OrderType.BUY, 50000, 50, true, null
        );

        List<OrderResponseMessage> responses = List.of(mock(OrderResponseMessage.class));
        when(repository.findAll("user1")).thenReturn(responses);

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/cancel"),
                argThat(resp -> resp instanceof CancelResultResponse
                        && ((CancelResultResponse) resp).responseType() == ResponseType.SUCCESS
                )
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/order"),
                eq(responses)
        );

        verify(repository).findAll("user1");
    }
}
