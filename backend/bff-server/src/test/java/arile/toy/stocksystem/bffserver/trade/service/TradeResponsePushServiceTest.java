package arile.toy.stocksystem.bffserver.trade.service;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.trade.dto.TradeResponse;
import arile.toy.stocksystem.bffserver.trade.dto.TradeType;
import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TradeResponsePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerOrderResponseRepository bffServerOrderResponseRepository;

    @InjectMocks
    private TradeResponsePushService service;

    @Test
    @DisplayName("체결 내역을 먼저 보내고, 이어서 체결이 반영된 미체결 목록을 보낸다")
    void push() {
        TradeResponseEvent event = new TradeResponseEvent(100L, 1L, "user1", "005930", TradeType.BUY,
                70_000, 3, Instant.parse("2026-09-24T00:31:00Z"));
        List<OrderResponseMessage> pending = List.of(mock(OrderResponseMessage.class));
        given(bffServerOrderResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event);

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trade", TradeResponse.fromEvent(event));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order", pending);
    }
}
