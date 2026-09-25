package arile.toy.stocksystem.stockserver.otoco.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoExitMode;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Consumer] OTOCO 등록 요청 처리(handle) 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOtocoRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private OtocoService otocoService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisOtocoRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisOtocoRequestEventConsumer(streamRedisTemplate, otocoService, registry, "otoco", "otoco-group", "A");
    }

    @DisplayName("PRICE 모드 값을 파싱해 등록을 요청한다 (\"null\" 문자열은 null, 대소문자 무시)")
    @Test
    void givenPriceMode_whenHandling_thenRegisters() {
        given(registry.isClosed("005930")).willReturn(false);

        sut.handle(record(Map.of("entryDirection", "above", "leverageRatio", "x2", "tpPct", "null", "slPct", "null")));

        then(otocoService).should().registerOtoco(new StockServerOtocoRequestEvent("user", "005930",
                OtocoEntryDirection.ABOVE, 10, 70_000, OtocoExitMode.PRICE, 73_500, null,
                OtocoExitMode.PRICE, 67_900, null, LeverageRatio.X2));
    }

    @DisplayName("PCT 모드는 비율 값을 사용하고, 레버리지 비율이 없으면 현물로 요청한다")
    @Test
    void givenPctMode_whenHandling_thenRegisters() {
        given(registry.isClosed("005930")).willReturn(false);
        Map<String, String> overrides = new HashMap<>();
        overrides.put("tpMode", "pct");
        overrides.put("tpPct", "5.0");
        overrides.put("slMode", "PCT");
        overrides.put("slPct", "3.0");

        sut.handle(record(overrides, "tpPrice", "slPrice", "leverageRatio"));

        then(otocoService).should().registerOtoco(new StockServerOtocoRequestEvent("user", "005930",
                OtocoEntryDirection.BELOW, 10, 70_000, OtocoExitMode.PCT, null, 5.0,
                OtocoExitMode.PCT, null, 3.0, LeverageRatio.SPOT));
    }

    @DisplayName("잘못된 방향·레버리지·모드이거나 모드에 필요한 값이 없으면 예외 없이 건너뛴다")
    @Test
    void givenInvalid_whenHandling_thenSkips() {
        sut.handle(record(Map.of("entryDirection", "SIDEWAYS")));
        sut.handle(record(Map.of("leverageRatio", "X10")));
        sut.handle(record(Map.of("tpMode", "MARKET")));
        sut.handle(record(Map.of(), "slMode"));
        sut.handle(record(Map.of(), "tpPrice"));
        sut.handle(record(Map.of("slMode", "PCT")));

        then(otocoService).should(never()).registerOtoco(any());
        then(registry).shouldHaveNoInteractions();
    }

    @DisplayName("장이 닫혀 있으면 등록하지 않는다")
    @Test
    void givenClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record(Map.of()));

        then(otocoService).should(never()).registerOtoco(any());
    }

    private MapRecord<String, Object, Object> record(Map<String, String> overrides, String... removed) {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTOCO_CREATED");
        value.put("username", "user");
        value.put("stockCode", "005930");
        value.put("entryDirection", "BELOW");
        value.put("leverageRatio", "SPOT");
        value.put("orderQuantity", "10");
        value.put("entryTriggerPrice", "70000");
        value.put("tpMode", "PRICE");
        value.put("tpPrice", "73500");
        value.put("slMode", "PRICE");
        value.put("slPrice", "67900");
        value.putAll(overrides);
        for (String key : removed) {
            value.remove(key);
        }
        return StreamRecords.newRecord().in("otoco-A").ofMap(value);
    }
}
