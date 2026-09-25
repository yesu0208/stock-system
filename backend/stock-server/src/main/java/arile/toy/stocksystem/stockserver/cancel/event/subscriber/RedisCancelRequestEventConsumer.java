package arile.toy.stocksystem.stockserver.cancel.event.subscriber;

import arile.toy.stocksystem.stockserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
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
public class RedisCancelRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final CancelService cancelService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisCancelRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            CancelService cancelService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.cancel.prefix}") String prefix,
            @Value("${redis.streams.cancel.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "CANCEL_CREATED", "cancel", "cancel-dlq");
        this.cancelService = cancelService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        Object orderIdObj = value.get("orderId");
        Long orderId = null;
        if (orderIdObj != null) {
            if (orderIdObj instanceof Number) {
                orderId = ((Number) orderIdObj).longValue();
            } else {
                orderId = Long.parseLong(orderIdObj.toString());
            }
        }

        String stockCode = (String) value.get("stockCode");
        // 요청자: 주문 소유자 검증에 사용 (bff가 JWT에서 추출해 전달)
        String username = (String) value.get("username");

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip cancel for stockCode {}", stockCode);
            return;
        }

        log.info("Processing cancel orderId: {} for stockCode {}", orderId, stockCode);

        cancelService.registerCancel(CancelRequestEvent.of(orderId, stockCode, username));
    }
}
