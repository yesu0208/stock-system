package arile.toy.stocksystem.bffserver.autocancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResultResponse;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
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
class AutoCancelResponsePushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;

    @InjectMocks
    private AutoCancelResponsePushService service;

    @Test
    @DisplayName("실패 이벤트 처리 시 에러 메시지 전송")
    void givenFailedEvent_whenPush_thenSendsErrorMessage() {
        // given
        AutoCancelResponseEvent event = new AutoCancelResponseEvent(
                1L,
                "user1",
                "005930",
                AutoOrderType.BUY,
                50000,
                50000,
                50,
                false,
                null
        );

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/cancel"),
                argThat(response -> response instanceof AutoCancelResultResponse
                        && ((AutoCancelResultResponse) response).responseType() == ResponseType.ERROR
                        && ((AutoCancelResultResponse) response).autoOrderId() == 1L
                )
        );

        verifyNoMoreInteractions(messagingTemplate);
        verifyNoInteractions(bffServerAutoOrderResponseRepository);
    }

    @Test
    @DisplayName("성공 이벤트 처리 시 성공 메시지와 주문 리스트 전송")
    void givenSuccessEvent_whenPush_thenSendsSuccessMessageAndOrderList() {
        // given
        AutoCancelResponseEvent event = new AutoCancelResponseEvent(
                1L,
                "user1",
                "005930",
                AutoOrderType.BUY,
                50000,
                50000,
                50,
                true,
                null
        );

        List<AutoOrderResponseMessage> mockResponses = List.of(mock(AutoOrderResponseMessage.class));
        when(bffServerAutoOrderResponseRepository.findAll("user1")).thenReturn(mockResponses);

        // when
        service.push(event);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/cancel"),
                argThat(response -> response instanceof AutoCancelResultResponse
                        && ((AutoCancelResultResponse) response).responseType() == ResponseType.SUCCESS
                        && ((AutoCancelResultResponse) response).autoOrderId() == 1L
                )
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/sub/auto/order"),
                eq(mockResponses)
        );

        verify(bffServerAutoOrderResponseRepository).findAll("user1");
    }
}
