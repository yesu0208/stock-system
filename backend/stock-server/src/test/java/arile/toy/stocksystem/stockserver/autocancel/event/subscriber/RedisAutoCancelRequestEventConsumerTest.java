package arile.toy.stocksystem.stockserver.autocancel.event.subscriber;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] 자동주문 취소 요청 컨슈머 설정·파싱 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAutoCancelRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private AutoCancelService autoCancelService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisAutoCancelRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisAutoCancelRequestEventConsumer(
                streamRedisTemplate, autoCancelService, registry, "auto-cancel", "auto-cancel-group", "1");
    }

    @DisplayName("스트림 키·그룹·이벤트 타입·키 구분자·DLQ를 자동주문 취소용으로 설정한다")
    @Test
    void whenCreated_thenConfiguresAutoCancelStream() {
        assertThat(ReflectionTestUtils.getField(sut, "streamKey")).isEqualTo("auto-cancel-1");
        assertThat(ReflectionTestUtils.getField(sut, "group")).isEqualTo("auto-cancel-group");
        assertThat(ReflectionTestUtils.getField(sut, "eventType")).isEqualTo("AUTO_CANCEL_CREATED");
        assertThat(ReflectionTestUtils.getField(sut, "keyNamespace")).isEqualTo("autoCancel");
        assertThat(ReflectionTestUtils.getField(sut, "dlqStreamKey")).isEqualTo("auto-cancel-dlq");
    }

    @DisplayName("자동주문 ID(문자열/숫자)·종목·요청자를 넘겨 취소를 요청한다")
    @Test
    void givenValidRecord_whenHandling_thenRegistersCancel() {
        Map<Object, Object> numericId = value();
        numericId.put("autoOrderId", 7);

        sut.handle(record(value()));
        sut.handle(record(numericId));

        then(autoCancelService).should().registerAutoCancel(AutoCancelRequestEvent.of(1L, "005930", "user"));
        then(autoCancelService).should().registerAutoCancel(AutoCancelRequestEvent.of(7L, "005930", "user"));
    }

    @DisplayName("장이 닫힌 종목이면 취소를 요청하지 않는다")
    @Test
    void givenMarketClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record(value()));

        then(autoCancelService).shouldHaveNoInteractions();
    }

    private Map<Object, Object> value() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "AUTO_CANCEL_CREATED");
        value.put("autoOrderId", "1");
        value.put("stockCode", "005930");
        value.put("username", "user");
        return value;
    }

    private MapRecord<String, Object, Object> record(Map<Object, Object> value) {
        return StreamRecords.newRecord()
                .in("auto-cancel-1")
                .withId(RecordId.of("1-0"))
                .ofMap(value);
    }
}
