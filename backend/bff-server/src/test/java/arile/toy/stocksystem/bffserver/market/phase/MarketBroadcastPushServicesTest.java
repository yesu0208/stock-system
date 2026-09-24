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

class MarketBroadcastPushServicesTest {

    private static ObjectMapper failingObjectMapper() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("boom") {});
        return objectMapper;
    }

    @Nested
    @DisplayName("레버리지 이자 청구 완료 알림")
    class InterestApplied {

        @Test
        @DisplayName("이자 청구 완료 메시지를 JSON 문자열로 바꿔 /sub/market/interest로 전체 전송한다")
        void push() {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

            new InterestAppliedPushService(messagingTemplate, new ObjectMapper().findAndRegisterModules()).push();

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/market/interest"), payload.capture());
            assertThat(payload.getValue()).isInstanceOf(String.class);
            assertThat((String) payload.getValue()).startsWith("{").endsWith("}");
        }

        @Test
        @DisplayName("JSON 변환에 실패해도 예외를 던지지 않고 전송하지 않는다")
        void serializationFails() throws JsonProcessingException {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
            InterestAppliedPushService service =
                    new InterestAppliedPushService(messagingTemplate, failingObjectMapper());

            assertThatCode(service::push).doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("랭크 갱신 완료 알림")
    class RankUpdated {

        @Test
        @DisplayName("랭크 갱신 완료 메시지를 JSON 문자열로 바꿔 /sub/market/rank로 전체 전송한다")
        void push() {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

            new RankUpdatedPushService(messagingTemplate, new ObjectMapper().findAndRegisterModules()).push();

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/market/rank"), payload.capture());
            assertThat(payload.getValue()).isInstanceOf(String.class);
            assertThat((String) payload.getValue()).startsWith("{").endsWith("}");
        }

        @Test
        @DisplayName("JSON 변환에 실패해도 예외를 던지지 않고 전송하지 않는다")
        void serializationFails() throws JsonProcessingException {
            SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
            RankUpdatedPushService service =
                    new RankUpdatedPushService(messagingTemplate, failingObjectMapper());

            assertThatCode(service::push).doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }
}
