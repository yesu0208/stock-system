package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.dto.OrderResultResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderResponsePushServiceTest {

    @Mock
    private BffServerOrderResponseRepository bffServerOrderResponseRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderResponsePushService service;

    @Test
    @DisplayName("실패 이벤트 처리 시 에러 메시지 전송")
    void givenFailedEvent_whenPush_thenSendsErrorMessage() {
        // given
        OrderResponseEvent event = new OrderResponseEvent(
                1L,
                "user1",
                "005930",
                OrderType.BUY,
                50000,
                10,
                null,
                false,
                null
        );

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/order/result"),
                argThat(response -> response instanceof OrderResultResponse
                        && ((OrderResultResponse) response).responseType() == ResponseType.ERROR
                        && ((OrderResultResponse) response).username().equals("user1")
                )
        );

        verifyNoMoreInteractions(messagingTemplate);
        verifyNoInteractions(bffServerOrderResponseRepository);
    }

    @Test
    @DisplayName("성공 이벤트 처리 시 성공 메시지와 주문 리스트 전송")
    void givenSuccessEvent_whenPush_thenSendsSuccessMessageAndOrderList() {
        // given
        OrderResponseEvent event = new OrderResponseEvent(
                1L,
                "user1",
                "005930",
                OrderType.BUY,
                50000,
                10,
                Instant.now(),
                true,
                null
        );

        List<OrderResponseMessage> mockResponses = List.of(mock(OrderResponseMessage.class));
        when(bffServerOrderResponseRepository.findAll("user1")).thenReturn(mockResponses);

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/order/result"),
                argThat(response -> response instanceof OrderResultResponse
                        && ((OrderResultResponse) response).responseType() == ResponseType.SUCCESS
                        && ((OrderResultResponse) response).username().equals("user1")
                )
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/order"),
                eq(mockResponses)
        );

        verify(bffServerOrderResponseRepository).findAll("user1");
    }
}
