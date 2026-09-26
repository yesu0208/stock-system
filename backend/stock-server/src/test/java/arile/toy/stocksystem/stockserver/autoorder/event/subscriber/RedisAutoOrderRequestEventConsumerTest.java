package arile.toy.stocksystem.stockserver.autoorder.event.subscriber;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] 자동주문 요청 컨슈머 설정·파싱 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAutoOrderRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private AutoOrderService autoOrderService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisAutoOrderRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisAutoOrderRequestEventConsumer(
                streamRedisTemplate, autoOrderService, registry, "auto-order", "auto-order-group", "1");
    }

    @DisplayName("스트림 키·그룹·이벤트 타입·키 구분자·DLQ를 자동주문용으로 설정한다")
    @Test
    void whenCreated_thenConfiguresAutoOrderStream() {
        assertThat(ReflectionTestUtils.getField(sut, "streamKey")).isEqualTo("auto-order-1");
        assertThat(ReflectionTestUtils.getField(sut, "group")).isEqualTo("auto-order-group");
        assertThat(ReflectionTestUtils.getField(sut, "eventType")).isEqualTo("AUTO_ORDER_CREATED");
        assertThat(ReflectionTestUtils.getField(sut, "keyNamespace")).isEqualTo("autoOrder");
        assertThat(ReflectionTestUtils.getField(sut, "dlqStreamKey")).isEqualTo("auto-order-dlq");
    }

    @DisplayName("정상 레코드는 파싱해 자동주문을 등록한다 (소문자 값도 파싱)")
    @Test
    void givenValidRecord_whenHandling_thenRegistersAutoOrder() {
        Map<Object, Object> value = autoOrderValue();
        value.put("autoOrderType", "sell");

        sut.handle(record(value));

        then(autoOrderService).should().registerAutoOrder(StockServerAutoOrderRequestEvent.of(
                "user", "005930", AutoOrderType.SELL, 69_000, 68_500, 10, LeverageRatio.X2));
    }

    @DisplayName("레버리지가 없으면 SPOT으로 간주한다")
    @Test
    void givenNoLeverage_whenHandling_thenSpot() {
        Map<Object, Object> value = autoOrderValue();
        value.remove("leverageRatio");

        sut.handle(record(value));

        then(autoOrderService).should().registerAutoOrder(StockServerAutoOrderRequestEvent.of(
                "user", "005930", AutoOrderType.BUY, 69_000, 68_500, 10, LeverageRatio.SPOT));
    }

    @DisplayName("자동주문 유형 또는 레버리지 값이 잘못되면 등록하지 않는다")
    @Test
    void givenInvalidEnum_whenHandling_thenSkips() {
        Map<Object, Object> invalidType = autoOrderValue();
        invalidType.put("autoOrderType", "HOLD");
        Map<Object, Object> invalidLeverage = autoOrderValue();
        invalidLeverage.put("leverageRatio", "X10");

        sut.handle(record(invalidType));
        sut.handle(record(invalidLeverage));

        then(autoOrderService).shouldHaveNoInteractions();
    }

    @DisplayName("장이 닫힌 종목이면 등록하지 않는다")
    @Test
    void givenMarketClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record(autoOrderValue()));

        then(autoOrderService).shouldHaveNoInteractions();
    }

    @DisplayName("자동 주문 타입이 없으면 예외 없이 건너뛴다 (재시도·DLQ 방지)")
    @Test
    void givenMissingType_whenHandling_thenSkips() {
        Map<Object, Object> value = autoOrderValue();
        value.remove("autoOrderType");

        assertThatNoException().isThrownBy(() -> sut.handle(record(value)));

        then(autoOrderService).should(never()).registerAutoOrder(any());
        then(registry).shouldHaveNoInteractions();
    }

    @DisplayName("주문가·발동가·수량이 없거나 숫자가 아니면 등록하지 않고 건너뛴다")
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(value = {"orderPrice, NULL", "triggerPrice, NULL", "orderQuantity, NULL", "orderPrice, abc"},
            nullValues = "NULL")
    void givenMissingOrInvalidNumber_whenHandling_thenSkips(String field, String rawValue) {
        Map<Object, Object> value = autoOrderValue();
        if (rawValue == null) {
            value.remove(field);
        } else {
            value.put(field, rawValue);
        }

        sut.handle(record(value));

        then(autoOrderService).should(never()).registerAutoOrder(any());
        then(registry).shouldHaveNoInteractions();
    }

    private Map<Object, Object> autoOrderValue() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "AUTO_ORDER_CREATED");
        value.put("username", "user");
        value.put("stockCode", "005930");
        value.put("autoOrderType", "BUY");
        value.put("triggerPrice", "69000");
        value.put("orderPrice", "68500");
        value.put("orderQuantity", "10");
        value.put("leverageRatio", "X2");
        return value;
    }

    private MapRecord<String, Object, Object> record(Map<Object, Object> value) {
        return StreamRecords.newRecord()
                .in("auto-order-1")
                .withId(RecordId.of("1-0"))
                .ofMap(value);
    }
}
