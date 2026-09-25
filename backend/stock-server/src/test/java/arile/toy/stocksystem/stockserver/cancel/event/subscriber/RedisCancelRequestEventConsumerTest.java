package arile.toy.stocksystem.stockserver.cancel.event.subscriber;

import arile.toy.stocksystem.stockserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
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

@DisplayName("[Consumer] 취소 요청 컨슈머 설정·파싱 테스트")
@ExtendWith(MockitoExtension.class)
class RedisCancelRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private CancelService cancelService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisCancelRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisCancelRequestEventConsumer(
                streamRedisTemplate, cancelService, registry, "cancel", "cancel-group", "1");
    }

    @DisplayName("스트림 키·그룹·이벤트 타입·키 구분자·DLQ를 취소용으로 설정한다")
    @Test
    void whenCreated_thenConfiguresCancelStream() {
        assertThat(ReflectionTestUtils.getField(sut, "streamKey")).isEqualTo("cancel-1");
        assertThat(ReflectionTestUtils.getField(sut, "group")).isEqualTo("cancel-group");
        assertThat(ReflectionTestUtils.getField(sut, "eventType")).isEqualTo("CANCEL_CREATED");
        assertThat(ReflectionTestUtils.getField(sut, "keyNamespace")).isEqualTo("cancel");
        assertThat(ReflectionTestUtils.getField(sut, "dlqStreamKey")).isEqualTo("cancel-dlq");
    }

    @DisplayName("정상 레코드는 주문 ID·종목·요청자를 넘겨 취소를 요청한다")
    @Test
    void givenValidRecord_whenHandling_thenRegistersCancel() {
        sut.handle(record(cancelValue()));

        then(cancelService).should().registerCancel(CancelRequestEvent.of(1L, "005930", "user"));
    }

    @DisplayName("주문 ID가 숫자 타입으로 들어와도 Long으로 변환한다")
    @Test
    void givenNumericOrderId_whenHandling_thenConvertsToLong() {
        Map<Object, Object> value = cancelValue();
        value.put("orderId", 7);

        sut.handle(record(value));

        then(cancelService).should().registerCancel(CancelRequestEvent.of(7L, "005930", "user"));
    }

    @DisplayName("주문 ID가 없으면 null로 넘긴다 (서비스에서 주문 없음 예외 → 재시도·DLQ)")
    @Test
    void givenMissingOrderId_whenHandling_thenPassesNull() {
        Map<Object, Object> value = cancelValue();
        value.remove("orderId");

        sut.handle(record(value));

        then(cancelService).should().registerCancel(CancelRequestEvent.of(null, "005930", "user"));
    }

    @DisplayName("장이 닫힌 종목이면 취소를 요청하지 않는다")
    @Test
    void givenMarketClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record(cancelValue()));

        then(cancelService).shouldHaveNoInteractions();
    }

    private Map<Object, Object> cancelValue() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "CANCEL_CREATED");
        value.put("orderId", "1");
        value.put("stockCode", "005930");
        value.put("username", "user");
        return value;
    }

    private MapRecord<String, Object, Object> record(Map<Object, Object> value) {
        return StreamRecords.newRecord()
                .in("cancel-1")
                .withId(RecordId.of("1-0"))
                .ofMap(value);
    }
}
