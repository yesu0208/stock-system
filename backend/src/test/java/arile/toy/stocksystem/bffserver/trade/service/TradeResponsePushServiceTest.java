package arile.toy.stocksystem.bffserver.trade.service;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.trade.dto.TradeResponse;
import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeResponsePushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BffServerOrderResponseRepository orderRepository;

    private TradeResponsePushService service;

    @BeforeEach
    void setup() {
        service = new TradeResponsePushService(messagingTemplate, orderRepository);
    }

    @Test
    @DisplayName("TradeResponseEvent 발생 시 사용자에게 Trade와 Order 모두 전송")
    void givenTradeResponseEvent_whenPush_thenSendToUserCalled() {
        // Given
        TradeResponseEvent event = mock(TradeResponseEvent.class);
        when(event.username()).thenReturn("user1");

        List<OrderResponseMessage> orderList = List.of(mock(OrderResponseMessage.class));
        when(orderRepository.findAll("user1")).thenReturn(orderList);

        // When
        service.push(event);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/trade"),
                any(TradeResponse.class)
        );

        verify(orderRepository).findAll("user1");

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/order"),
                eq(orderList)
        );
    }

    @Test
    @DisplayName("주문이 없어도 Trade 이벤트는 사용자에게 전송")
    void givenNoOrders_whenPush_thenStillSendTradeEvent() {
        // Given
        TradeResponseEvent event = mock(TradeResponseEvent.class);
        when(event.username()).thenReturn("user1");

        when(orderRepository.findAll("user1")).thenReturn(List.of());

        // When
        service.push(event);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/trade"),
                any(TradeResponse.class)
        );

        verify(orderRepository).findAll("user1");

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/order"),
                eq(List.of())
        );
    }
}
