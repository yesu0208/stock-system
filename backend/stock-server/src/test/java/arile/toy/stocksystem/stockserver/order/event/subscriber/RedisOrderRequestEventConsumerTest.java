package arile.toy.stocksystem.stockserver.order.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] 주문 요청 컨슈머 설정·파싱 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOrderRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private OrderService orderService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisOrderRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisOrderRequestEventConsumer(
                streamRedisTemplate, orderService, registry, "order", "order-group", "1");
    }

    @DisplayName("스트림 키·그룹·이벤트 타입·키 구분자·DLQ를 주문용으로 설정한다")
    @Test
    void whenCreated_thenConfiguresOrderStream() {
        assertThat(ReflectionTestUtils.getField(sut, "streamKey")).isEqualTo("order-1");
        assertThat(ReflectionTestUtils.getField(sut, "group")).isEqualTo("order-group");
        assertThat(ReflectionTestUtils.getField(sut, "eventType")).isEqualTo("ORDER_CREATED");
        assertThat(ReflectionTestUtils.getField(sut, "keyNamespace")).isEqualTo("order");
        assertThat(ReflectionTestUtils.getField(sut, "dlqStreamKey")).isEqualTo("order-dlq");
    }

    @DisplayName("정상 레코드는 파싱해 주문을 등록한다")
    @Test
    void givenValidRecord_whenHandling_thenRegistersOrder() {
        sut.handle(record(orderValue()));

        ArgumentCaptor<StockServerOrderRequestEvent> captor =
                ArgumentCaptor.forClass(StockServerOrderRequestEvent.class);
        then(orderService).should().registerOrder(captor.capture(), eq(false));
        StockServerOrderRequestEvent event = captor.getValue();
        assertThat(event.username()).isEqualTo("user");
        assertThat(event.stockCode()).isEqualTo("005930");
        assertThat(event.orderType()).isEqualTo(OrderType.BUY);
        assertThat(event.orderPrice()).isEqualTo(70_000);
        assertThat(event.orderQuantity()).isEqualTo(10);
        assertThat(event.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(event.orderExecutionType()).isEqualTo(OrderExecutionType.MARKET);
    }

    @DisplayName("레버리지·체결유형이 없으면 SPOT·LIMIT으로 간주하고, 소문자 값도 파싱한다")
    @Test
    void givenMissingOptionalFields_whenHandling_thenUsesDefaults() {
        Map<Object, Object> value = orderValue();
        value.remove("leverageRatio");
        value.remove("orderExecutionType");
        value.put("orderType", "sell");

        sut.handle(record(value));

        ArgumentCaptor<StockServerOrderRequestEvent> captor =
                ArgumentCaptor.forClass(StockServerOrderRequestEvent.class);
        then(orderService).should().registerOrder(captor.capture(), eq(false));
        assertThat(captor.getValue().orderType()).isEqualTo(OrderType.SELL);
        assertThat(captor.getValue().leverageRatio()).isEqualTo(LeverageRatio.SPOT);
        assertThat(captor.getValue().orderExecutionType()).isEqualTo(OrderExecutionType.LIMIT);
    }

    @DisplayName("주문유형이 잘못되면 주문을 등록하지 않는다")
    @Test
    void givenInvalidOrderType_whenHandling_thenSkips() {
        Map<Object, Object> value = orderValue();
        value.put("orderType", "HOLD");

        sut.handle(record(value));

        then(orderService).shouldHaveNoInteractions();
    }

    @DisplayName("레버리지 값이 잘못되면 주문을 등록하지 않는다")
    @Test
    void givenInvalidLeverage_whenHandling_thenSkips() {
        Map<Object, Object> value = orderValue();
        value.put("leverageRatio", "X10");

        sut.handle(record(value));

        then(orderService).shouldHaveNoInteractions();
    }

    @DisplayName("체결유형 값이 잘못되면 주문을 등록하지 않는다")
    @Test
    void givenInvalidExecutionType_whenHandling_thenSkips() {
        Map<Object, Object> value = orderValue();
        value.put("orderExecutionType", "STOP");

        sut.handle(record(value));

        then(orderService).shouldHaveNoInteractions();
    }

    @DisplayName("장이 닫힌 종목이면 주문을 등록하지 않는다")
    @Test
    void givenMarketClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record(orderValue()));

        then(orderService).shouldHaveNoInteractions();
    }

    @DisplayName("orderType이 없으면 예외를 던져 재시도 대상이 된다")
    @Test
    void givenMissingOrderType_whenHandling_thenThrows() {
        Map<Object, Object> value = orderValue();
        value.remove("orderType");

        assertThatThrownBy(() -> sut.handle(record(value)))
                .isInstanceOf(NullPointerException.class);

        then(orderService).shouldHaveNoInteractions();
    }

    private Map<Object, Object> orderValue() {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "ORDER_CREATED");
        value.put("username", "user");
        value.put("stockCode", "005930");
        value.put("orderType", "BUY");
        value.put("orderPrice", "70000");
        value.put("orderQuantity", "10");
        value.put("leverageRatio", "X2");
        value.put("orderExecutionType", "MARKET");
        return value;
    }

    private MapRecord<String, Object, Object> record(Map<Object, Object> value) {
        return StreamRecords.newRecord()
                .in("order-1")
                .withId(RecordId.of("1-0"))
                .ofMap(value);
    }
}
