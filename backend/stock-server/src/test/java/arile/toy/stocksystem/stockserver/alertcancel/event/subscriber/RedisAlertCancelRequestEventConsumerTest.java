package arile.toy.stocksystem.stockserver.alertcancel.event.subscriber;

import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.stockserver.alertcancel.service.AlertCancelService;
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

import static org.mockito.BDDMockito.then;

@DisplayName("[Consumer] 알림 취소 요청 처리(handle) 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAlertCancelRequestEventConsumerTest {

    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private AlertCancelService alertCancelService;

    private RedisAlertCancelRequestEventConsumer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisAlertCancelRequestEventConsumer(streamRedisTemplate, alertCancelService,
                "alert-cancel", "alert-cancel-group", "A");
    }

    @DisplayName("문자열·숫자 id를 모두 파싱해 취소를 요청하고, id가 없으면 null로 전달한다")
    @Test
    void givenRecords_whenHandling_thenCancels() {
        sut.handle(record("1"));
        sut.handle(record(2L));
        sut.handle(record(null));

        then(alertCancelService).should().registerAlertCancel(AlertCancelRequestEvent.of(1L, "005930", "user"));
        then(alertCancelService).should().registerAlertCancel(AlertCancelRequestEvent.of(2L, "005930", "user"));
        then(alertCancelService).should().registerAlertCancel(AlertCancelRequestEvent.of(null, "005930", "user"));
    }

    private MapRecord<String, Object, Object> record(Object alertId) {
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "ALERT_CANCEL_CREATED");
        value.put("stockCode", "005930");
        value.put("username", "user");
        if (alertId != null) {
            value.put("alertId", alertId);
        }
        return StreamRecords.newRecord().in("alert-cancel-A").ofMap(value);
    }
}
