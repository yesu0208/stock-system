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

        AlertDirection direction;
        try {
            direction = AlertDirection.valueOf(directionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid direction: {}", directionStr);
            return;
        }

        Object rawTriggerPrice = value.get("triggerPrice");
        Integer triggerPrice = null;

        if (rawTriggerPrice != null) {
            triggerPrice = Integer.parseInt(rawTriggerPrice.toString());
        }

        log.info("Processing alert username: {} for stock {}", username, stockCode);

        alertService.registerAlert(AlertRequestEvent.of(username, stockCode, direction, triggerPrice));
    }
}
