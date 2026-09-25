package arile.toy.stocksystem.stockserver.trailingstopcancel.event.subscriber;

import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.service.TrailingStopCancelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisTrailingStopCancelRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final TrailingStopCancelService trailingStopCancelService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisTrailingStopCancelRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            TrailingStopCancelService trailingStopCancelService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.trailing-stop-cancel.prefix}") String prefix,
            @Value("${redis.streams.trailing-stop-cancel.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "TRAILING_STOP_CANCEL_CREATED", "trailingStopCancel", "trailing-stop-cancel-dlq");
        this.trailingStopCancelService = trailingStopCancelService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        Object trailingStopIdObj = value.get("trailingStopId");
        Long trailingStopId = null;
        if (trailingStopIdObj != null) {
            trailingStopId = trailingStopIdObj instanceof Number
                    ? ((Number) trailingStopIdObj).longValue()
                    : Long.parseLong(trailingStopIdObj.toString());
        }

        String stockCode = (String) value.get("stockCode");
        // 요청자: 트레일링 스탑 소유자 검증에 사용 (bff가 JWT에서 추출해 전달)
        String username = (String) value.get("username");

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip trailing stop cancel for stockCode {}", stockCode);
            return;
        }

        log.info("Processing trailing stop cancel trailingStopId: {} for stockCode {}", trailingStopId, stockCode);

        trailingStopCancelService.registerCancel(TrailingStopCancelRequestEvent.of(trailingStopId, stockCode, username));
    }
}
