package arile.toy.stocksystem.stockserver.alertcancel.event.subscriber;

import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.stockserver.alertcancel.service.AlertCancelService;
import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisAlertCancelRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final AlertCancelService alertCancelService;

    public RedisAlertCancelRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            AlertCancelService alertCancelService,
            @Value("${redis.streams.alert-cancel.prefix}") String prefix,
            @Value("${redis.streams.alert-cancel.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "ALERT_CANCEL_CREATED", "alertCancel", "alert-cancel-dlq");
        this.alertCancelService = alertCancelService;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        Object alertIdObj = value.get("alertId");
        Long alertId = null;
        if (alertIdObj != null) {
            if (alertIdObj instanceof Number) {
                alertId = ((Number) alertIdObj).longValue();
            } else {
                alertId = Long.parseLong(alertIdObj.toString());
            }
        }

        String stockCode = (String) value.get("stockCode");
        // 요청자: 알림 소유자 검증에 사용 (bff가 JWT에서 추출해 전달)
        String username = (String) value.get("username");

        log.info("Processing alert cancel alertId: {} for stockCode {}", alertId, stockCode);

        alertCancelService.registerAlertCancel(AlertCancelRequestEvent.of(alertId, stockCode, username));
    }
}
