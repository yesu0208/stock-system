package arile.toy.stocksystem.stockserver.trailingstop.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] 트레일링 스탑 등록 요청 처리(handle) 테스트")
@ExtendWith(MockitoExtension.class)
class RedisTrailingStopRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private TrailingStopService trailingStopService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisTrailingStopRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisTrailingStopRequestEventConsumer(streamRedisTemplate, trailingStopService, registry,
                "trailing-stop", "trailing-stop-group", "A");
    }

    @DisplayName("값을 파싱해 트레일링 스탑 등록을 요청한다 (대소문자 무시)")
    @Test
    void givenValidRecord_whenHandling_thenRegisters() {
        given(registry.isClosed("005930")).willReturn(false);

        sut.handle(record(Map.of("leverageRatio", "x2", "trailingStopType", "sell")));

        then(trailingStopService).should().registerTrailingStop(StockServerTrailingStopRequestEvent
                .of("user", "005930", TrailingStopType.SELL, 10, 3.0, 70_000, LeverageRatio.X2));
    }

    @DisplayName("레버리지 비율이 없으면 현물로 요청한다")
    @Test
    void givenNoLeverage_whenHandling_thenSpot() {
        given(registry.isClosed("005930")).willReturn(false);

        sut.handle(record(Map.of()));

        then(trailingStopService).should().registerTrailingStop(StockServerTrailingStopRequestEvent
                .of("user", "005930", TrailingStopType.BUY, 10, 3.0, 70_000, LeverageRatio.SPOT));
    }

    @DisplayName("잘못된 타입·레버리지 비율이거나 장이 닫혀 있으면 등록하지 않는다")
    @Test
    void givenInvalidOrClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record(Map.of("trailingStopType", "HOLD")));
        sut.handle(record(Map.of("leverageRatio", "X10")));
        sut.handle(record(Map.of()));

        then(trailingStopService).should(never()).registerTrailingStop(any());
    }

    @DisplayName("트레일링 스탑 타입이 없으면 예외 없이 건너뛴다")
    @Test
    void givenMissingTrailingStopType_whenHandling_thenSkips() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("trailingStopType", null);

        assertThatNoException().isThrownBy(() -> sut.handle(record(overrides)));

        then(trailingStopService).shouldHaveNoInteractions();
        then(registry).shouldHaveNoInteractions();
    }

    @DisplayName("수량·비율·기준가가 없거나 숫자가 아니면 예외 없이 건너뛴다")
    @ParameterizedTest
    @CsvSource(value = {
            "orderQuantity, NULL",
            "orderQuantity, abc",
            "stopPercent, NULL",
            "stopPercent, abc",
            "basePrice, NULL",
            "basePrice, 1.5"
    }, nullValues = "NULL")
    void givenInvalidNumber_whenHandling_thenSkips(String field, String raw) {
        Map<String, String> overrides = new HashMap<>();
        overrides.put(field, raw);

        assertThatNoException().isThrownBy(() -> sut.handle(record(overrides)));

        then(trailingStopService).shouldHaveNoInteractions();
        then(registry).shouldHaveNoInteractions();
    }

    // overrides의 값이 null이면 해당 필드를 레코드에서 제거
    private MapRecord<String, Object, Object> record(Map<String, String> overrides) {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "TRAILING_STOP_CREATED");
        value.put("username", "user");
        value.put("stockCode", "005930");
        value.put("trailingStopType", "BUY");
        value.put("orderQuantity", "10");
        value.put("stopPercent", "3.0");
        value.put("basePrice", "70000");
        value.putAll(overrides);
        value.values().removeIf(v -> v == null);
        return StreamRecords.newRecord().in("trailing-stop-A").ofMap(value);
    }
}
