package arile.toy.stocksystem.stockserver.autocancel.event.subscriber;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisAutoCancelRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final AutoCancelService autoCancelService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisAutoCancelRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            AutoCancelService autoCancelService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.auto-cancel.prefix}") String prefix,
            @Value("${redis.streams.auto-cancel.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "AUTO_CANCEL_CREATED", "autoCancel", "auto-cancel-dlq");
        this.autoCancelService = autoCancelService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        Object autoOrderIdObj = value.get("autoOrderId");
        Long autoOrderId = null;
        if (autoOrderIdObj != null) {
            if (autoOrderIdObj instanceof Number) {
                autoOrderId = ((Number) autoOrderIdObj).longValue();
            } else {
                autoOrderId = Long.parseLong(autoOrderIdObj.toString());
            }
        }

        String stockCode = (String) value.get("stockCode");
        // 요청자: 자동 주문 소유자 검증에 사용 (bff가 JWT에서 추출해 전달)
        String username = (String) value.get("username");

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip auto cancel for stockCode {}", stockCode);
            return;
        }

        log.info("Processing cancel autoOrderId: {} for stockCode {}", autoOrderId, stockCode);

        autoCancelService.registerAutoCancel(AutoCancelRequestEvent.of(autoOrderId, stockCode, username));
    }
}
