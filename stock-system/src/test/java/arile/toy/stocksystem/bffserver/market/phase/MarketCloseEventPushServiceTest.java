package arile.toy.stocksystem.bffserver.market.phase;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class MarketCloseEventPushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MarketCloseEventPushService service;

    @Test
    @DisplayName("Market close 이벤트 메시지 전송")
    void givenMessage_whenPush_thenSendsMessage() {
        // given
        String message = "Market is closed";

        // when
        service.push(message);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/sub/market/close"),
                eq(message)
        );

        verifyNoMoreInteractions(messagingTemplate);
    }
}
