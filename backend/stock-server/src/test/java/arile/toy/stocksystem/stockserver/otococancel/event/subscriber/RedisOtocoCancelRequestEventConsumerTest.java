package arile.toy.stocksystem.stockserver.otococancel.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.otococancel.service.OtocoCancelService;
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

@DisplayName("[Consumer] OTOCO 취소 요청 처리(handle) 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOtocoCancelRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private OtocoCancelService otocoCancelService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisOtocoCancelRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisOtocoCancelRequestEventConsumer(streamRedisTemplate, otocoCancelService, registry,
                "otoco-cancel", "otoco-cancel-group", "A");
    }

    @DisplayName("문자열·숫자 id를 모두 파싱해 취소를 요청하고, id가 없으면 null로 전달한다")
    @Test
    void givenRecords_whenHandling_thenCancels() {
        given(registry.isClosed("005930")).willReturn(false);

        sut.handle(record("1"));
        sut.handle(record(2L));
        sut.handle(record(null));

        then(otocoCancelService).should().registerCancel(OtocoCancelRequestEvent.of(1L, "005930", "user"));
        then(otocoCancelService).should().registerCancel(OtocoCancelRequestEvent.of(2L, "005930", "user"));
        then(otocoCancelService).should().registerCancel(OtocoCancelRequestEvent.of(null, "005930", "user"));
    }

    @DisplayName("장이 닫혀 있으면 취소를 요청하지 않는다")
    @Test
    void givenClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record("1"));

        then(otocoCancelService).should(never()).registerCancel(any());
    }

    private MapRecord<String, Object, Object> record(Object otocoId) {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTOCO_CANCEL_CREATED");
        value.put("stockCode", "005930");
        value.put("username", "user");
        if (otocoId != null) {
            value.put("otocoId", otocoId);
        }
        return StreamRecords.newRecord().in("otoco-cancel-A").ofMap(value);
    }
}
