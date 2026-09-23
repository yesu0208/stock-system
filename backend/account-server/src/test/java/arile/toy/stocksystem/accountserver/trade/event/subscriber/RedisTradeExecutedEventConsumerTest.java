package arile.toy.stocksystem.accountserver.trade.event.subscriber;

import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.trade.service.TradeExecutionApplyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RedisTradeExecutedEventConsumerTest {

    private static final String PAYLOAD = "{\"trade\":\"json\"}";

    @Mock
    private RedisTemplate<String, Object> streamRedisTemplate;

    @Mock
    private TradeExecutionApplyService tradeExecutionApplyService;

    @Mock
    private ObjectMapper objectMapper;

    private RedisTradeExecutedEventConsumer consumer() {
        return new RedisTradeExecutedEventConsumer(
                streamRedisTemplate, tradeExecutionApplyService, objectMapper,
                "trade-executed-events", "account-group");
    }

    private static MapRecord<String, Object, Object> record() {
        return StreamRecords.newRecord()
                .in("trade-executed-events")
                .withId(RecordId.of("1-0"))
                .ofMap(Map.<Object, Object>of("type", "TRADE_EXECUTED", "payload", PAYLOAD));
    }

    @Test
    @DisplayName("TRADE_EXECUTED 이벤트를 처리하고 전용 processed 키와 DLQ를 사용한다")
    void metadata() {
        RedisTradeExecutedEventConsumer consumer = consumer();

        assertThat(consumer.eventType()).isEqualTo("TRADE_EXECUTED");
        assertThat(consumer.processedKeyPrefix()).isEqualTo("processed:tradeExecuted:");
        assertThat(consumer.dlqStreamKey()).isEqualTo("trade-executed-dlq");
    }

    @Test
    @DisplayName("handle: payload를 TradeExecutedEvent로 역직렬화해 체결을 반영한다")
    void handle_appliesTrade() throws Exception {
        TradeExecutedEvent event = mock(TradeExecutedEvent.class);
        given(objectMapper.readValue(PAYLOAD, TradeExecutedEvent.class)).willReturn(event);

        consumer().handle(record());

        verify(tradeExecutionApplyService).apply(event);
    }

    @Test
    @DisplayName("handle: payload 파싱에 실패하면 IllegalStateException을 던진다 (재시도 대상)")
    void handle_whenParseFails_throws() throws Exception {
        given(objectMapper.readValue(PAYLOAD, TradeExecutedEvent.class))
                .willThrow(new JsonProcessingException("invalid") {});

        assertThatThrownBy(() -> consumer().handle(record()))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(JsonProcessingException.class);

        verifyNoInteractions(tradeExecutionApplyService);
    }

    @Test
    @DisplayName("handle: 체결 반영 중 예외가 나면 IllegalStateException으로 감싸 던진다 (재시도 대상)")
    void handle_whenApplyFails_throws() throws Exception {
        TradeExecutedEvent event = mock(TradeExecutedEvent.class);
        given(objectMapper.readValue(PAYLOAD, TradeExecutedEvent.class)).willReturn(event);
        RuntimeException cause = new RuntimeException("db down");
        willThrow(cause).given(tradeExecutionApplyService).apply(event);

        assertThatThrownBy(() -> consumer().handle(record()))
                .isInstanceOf(IllegalStateException.class)
                .hasCause(cause);
    }
}
