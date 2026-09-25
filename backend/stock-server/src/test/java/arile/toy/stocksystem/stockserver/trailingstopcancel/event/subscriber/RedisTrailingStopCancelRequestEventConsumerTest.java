package arile.toy.stocksystem.stockserver.trailingstopcancel.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.service.TrailingStopCancelService;
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

@DisplayName("[Consumer] 트레일링 스탑 취소 요청 처리(handle) 테스트")
@ExtendWith(MockitoExtension.class)
class RedisTrailingStopCancelRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private TrailingStopCancelService trailingStopCancelService;
    @Mock private StockServerMarketPhaseRegistry registry;

    private RedisTrailingStopCancelRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisTrailingStopCancelRequestEventConsumer(streamRedisTemplate, trailingStopCancelService, registry,
                "trailing-stop-cancel", "trailing-stop-cancel-group", "A");
    }

    @DisplayName("문자열·숫자 id를 모두 파싱해 취소를 요청한다")
    @Test
    void givenValidRecord_whenHandling_thenCancels() {
        given(registry.isClosed("005930")).willReturn(false);

        sut.handle(record("1"));
        sut.handle(record(2L));

        then(trailingStopCancelService).should().registerCancel(TrailingStopCancelRequestEvent.of(1L, "005930", "user"));
        then(trailingStopCancelService).should().registerCancel(TrailingStopCancelRequestEvent.of(2L, "005930", "user"));
    }

    @DisplayName("id가 없으면 null로 전달한다")
    @Test
    void givenNoId_whenHandling_thenNullId() {
        given(registry.isClosed("005930")).willReturn(false);

        sut.handle(record(null));

        then(trailingStopCancelService).should().registerCancel(TrailingStopCancelRequestEvent.of(null, "005930", "user"));
    }

    @DisplayName("장이 닫혀 있으면 취소를 요청하지 않는다")
    @Test
    void givenClosed_whenHandling_thenSkips() {
        given(registry.isClosed("005930")).willReturn(true);

        sut.handle(record("1"));

        then(trailingStopCancelService).should(never()).registerCancel(any());
    }

    private MapRecord<String, Object, Object> record(Object trailingStopId) {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "TRAILING_STOP_CANCEL_CREATED");
        value.put("stockCode", "005930");
        value.put("username", "user");
        if (trailingStopId != null) {
            value.put("trailingStopId", trailingStopId);
        }
        return StreamRecords.newRecord().in("trailing-stop-cancel-A").ofMap(value);
    }
}
