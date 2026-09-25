package arile.toy.stocksystem.stockserver.alert.event.subscriber;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.service.AlertService;
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

@DisplayName("[Consumer] 알림 등록 요청 처리(handle) 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAlertRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private AlertService alertService;

    private RedisAlertRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisAlertRequestEventConsumer(streamRedisTemplate, alertService, "alert", "alert-group", "A");
    }

    @DisplayName("방향(대소문자 무시)과 발동가를 파싱해 등록을 요청한다")
    @Test
    void givenValidRecord_whenHandling_thenRegisters() {
        sut.handle(record("below", "68000"));

        then(alertService).should().registerAlert(AlertRequestEvent.of("user", "005930", AlertDirection.BELOW, 68_000));
    }

    @DisplayName("방향이 없거나 잘못됐거나, 발동가가 없거나 숫자가 아니면 예외 없이 건너뛴다")
    @Test
    void givenInvalid_whenHandling_thenSkips() {
        sut.handle(record(null, "68000"));
        sut.handle(record("SIDEWAYS", "68000"));
        sut.handle(record("ABOVE", null));
        sut.handle(record("ABOVE", "abc"));

        then(alertService).should(never()).registerAlert(any());
    }

    private MapRecord<String, Object, Object> record(String direction, String triggerPrice) {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "ALERT_CREATED");
        value.put("username", "user");
        value.put("stockCode", "005930");
        if (direction != null) value.put("direction", direction);
        if (triggerPrice != null) value.put("triggerPrice", triggerPrice);
        return StreamRecords.newRecord().in("alert-A").ofMap(value);
    }
}
