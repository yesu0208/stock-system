package arile.toy.stocksystem.bffserver.market.phase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MarketPhasePushServicesTest {

    @Nested
    @DisplayName("전체 장 상태 푸시")
    class GlobalPhase {

        @Test
        @DisplayName("장 상태를 JSON 문자열로 바꿔 /sub/market/phase로 전체 전송한다")
        void push() {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

            new GlobalMarketPhasePushService(messagingTemplate, new ObjectMapper().findAndRegisterModules())
                    .push("CLOSING_CALL");

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/market/phase"), payload.capture());
            assertThat((String) payload.getValue()).contains("CLOSING_CALL");
        }

        @Test
        @DisplayName("알 수 없는 장 상태면 예외 없이 전송하지 않는다")
        void unknownPhase() {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
            GlobalMarketPhasePushService service =
                    new GlobalMarketPhasePushService(messagingTemplate, new ObjectMapper());

            assertThatCode(() -> service.push("LUNCH")).doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("장 마감 푸시")
    class MarketClose {

        @Test
        @DisplayName("세션 구분을 담은 장 마감 메시지를 JSON 문자열로 바꿔 /sub/market/close로 전체 전송한다")
        void push() {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

            new MarketCloseEventPushService(messagingTemplate, new ObjectMapper().findAndRegisterModules())
                    .push("REGULAR");

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/market/close"), payload.capture());
            assertThat((String) payload.getValue()).contains("REGULAR");
        }

        @Test
        @DisplayName("JSON 변환에 실패해도 예외 없이 전송하지 않는다")
        void serializationFails() throws JsonProcessingException {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
            ObjectMapper objectMapper = mock(ObjectMapper.class);
            given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("boom") {});

            MarketCloseEventPushService service = new MarketCloseEventPushService(messagingTemplate, objectMapper);

            assertThatCode(() -> service.push("REGULAR")).doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }
}
