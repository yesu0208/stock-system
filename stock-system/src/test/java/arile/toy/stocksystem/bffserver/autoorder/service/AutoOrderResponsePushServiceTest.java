package arile.toy.stocksystem.bffserver.autoorder.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultResponse;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoOrderResponsePushServiceTest {

    @Mock
    private BffServerAutoOrderResponseRepository repository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AutoOrderResponsePushService service;

    @Test
    @DisplayName("실패 이벤트 처리 시 에러 메시지 전송")
    void givenFailedEvent_whenPush_thenSendsErrorMessage() {
        // given
        AutoOrderResponseEvent event = new AutoOrderResponseEvent(
                1L, "user1", "005930",
                AutoOrderType.BUY, 50000, 50000, 50,
                Instant.now(), false, null
        );

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/order/result"),
                argThat(resp -> resp instanceof AutoOrderResultResponse
                        && ((AutoOrderResultResponse) resp).responseType() == ResponseType.ERROR
                )
        );
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("트리거 이벤트 처리 시 주문 리스트만 전송")
    void givenTriggeredEvent_whenPush_thenSendsOrderList() {
        // given
        AutoOrderResponseEvent event = new AutoOrderResponseEvent(
                1L, "user1", "005930",
                AutoOrderType.BUY, 50000, 50000, 50,
                Instant.now(), true, AutoOrderResultCode.TRIGGERED
        );

        List<AutoOrderResponseMessage> responses = List.of(mock(AutoOrderResponseMessage.class));
        when(repository.findAll("user1")).thenReturn(responses);

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/order"),
                eq(responses)
        );

        verify(repository).findAll("user1");
    }

    @Test
    @DisplayName("성공 이벤트 처리 시 성공 메시지와 주문 리스트 전송")
    void givenSuccessEvent_whenPush_thenSendsSuccessMessageAndOrderList() {
        // given
        Instant orderTime = Instant.now();
        AutoOrderResponseEvent event = new AutoOrderResponseEvent(
                1L, "user1", "005930",
                AutoOrderType.BUY, 50000, 50000, 50,
                orderTime, true, null
        );

        List<AutoOrderResponseMessage> responses = List.of(mock(AutoOrderResponseMessage.class));
        when(repository.findAll("user1")).thenReturn(responses);

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/order/result"),
                argThat(resp -> resp instanceof AutoOrderResultResponse
                        && ((AutoOrderResultResponse) resp).responseType() == ResponseType.SUCCESS
                        && ((AutoOrderResultResponse) resp).orderTime().equals(orderTime)
                )
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/order"),
                eq(responses)
        );

        verify(repository).findAll("user1");
    }
}
