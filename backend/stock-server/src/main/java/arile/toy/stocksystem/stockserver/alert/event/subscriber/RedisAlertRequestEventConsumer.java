package arile.toy.stocksystem.stockserver.alert.event.subscriber;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.service.AlertService;
import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisAlertRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final AlertService alertService;

    public RedisAlertRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            AlertService alertService,
            @Value("${redis.streams.alert.prefix}") String prefix,
            @Value("${redis.streams.alert.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "ALERT_CREATED", "alert", "alert-dlq");
        this.alertService = alertService;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String username = (String) value.get("username");
        String stockCode = (String) value.get("stockCode");
        String directionStr = (String) value.get("direction");

        // 잘못된 값은 재시도해도 같은 결과이므로 예외(재시도·DLQ) 대신 로그만 남기고 건너뜀
        AlertDirection direction;
        try {
            direction = AlertDirection.valueOf(directionStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid direction: {}", directionStr);
            return;
        }

        Object rawTriggerPrice = value.get("triggerPrice");
        Integer triggerPrice;
        try {
            triggerPrice = Integer.parseInt(rawTriggerPrice.toString());
        } catch (NumberFormatException | NullPointerException e) {
            // 가격이 없으면 저장 시 not null 제약 위반으로 매번 실패함
            log.error("Invalid triggerPrice: {}", rawTriggerPrice);
            return;
        }

        log.info("Processing alert username: {} for stock {}", username, stockCode);

        alertService.registerAlert(AlertRequestEvent.of(username, stockCode, direction, triggerPrice));
    }
}
