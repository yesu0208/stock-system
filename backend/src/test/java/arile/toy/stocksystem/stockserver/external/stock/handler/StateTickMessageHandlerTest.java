package arile.toy.stocksystem.stockserver.external.stock.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateTickMessageHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonNode rootNode;

    @Mock
    private JsonNode bodyNode;

    @Mock
    private JsonNode headerNode;

    @InjectMocks
    private StateTickMessageHandler handler;

    @BeforeEach
    void setup() {
    }

    @Test
    @DisplayName("SUBSCRIBE SUCCESS 메시지를 처리하면 로그를 기록한다")
    void givenSubscribeSuccessMessage_whenHandle_thenLogInfo() throws JsonProcessingException {
        // given
        String json = "{\"body\":{\"msg1\":\"SUBSCRIBE SUCCESS\"}}";

        when(objectMapper.readTree(json)).thenReturn(rootNode);
        when(rootNode.path("body")).thenReturn(bodyNode);
        when(bodyNode.has("msg1")).thenReturn(true);
        when(bodyNode.path("msg1")).thenReturn(mock(JsonNode.class));
        when(bodyNode.path("msg1").asText()).thenReturn("SUBSCRIBE SUCCESS");

        // when
        handler.handle(json);

        // then
        verify(objectMapper).readTree(json);
    }

    @Test
    @DisplayName("PINGPONG 메시지를 처리하면 로그를 기록한다")
    void givenPingpongMessage_whenHandle_thenLogInfo() throws JsonProcessingException {
        // given
        String json = "{\"header\":{\"tr_id\":\"PINGPONG\"}}";

        when(objectMapper.readTree(json)).thenReturn(rootNode);
        when(rootNode.path("body")).thenReturn(mock(JsonNode.class));
        when(rootNode.path("body").has("msg1")).thenReturn(false);
        when(rootNode.path("header")).thenReturn(headerNode);
        when(headerNode.path("tr_id")).thenReturn(mock(JsonNode.class));
        when(headerNode.path("tr_id").asText()).thenReturn("PINGPONG");

        // when
        handler.handle(json);

        // then
        verify(objectMapper).readTree(json);
    }

    @Test
    @DisplayName("잘못된 JSON 메시지를 처리하면 경고 로그를 기록한다")
    void givenInvalidJson_whenHandle_thenLogWarn() throws JsonProcessingException {
        // given
        String json = "INVALID_JSON";

        when(objectMapper.readTree(json)).thenThrow(JsonProcessingException.class);

        // when
        handler.handle(json);

        // then
        verify(objectMapper).readTree(json);
    }
}
