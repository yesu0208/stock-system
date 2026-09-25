package arile.toy.stocksystem.stockserver.trailingstop.event.subscriber;

import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisTrailingStopRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final TrailingStopService trailingStopService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisTrailingStopRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            TrailingStopService trailingStopService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.trailing-stop.prefix}") String prefix,
            @Value("${redis.streams.trailing-stop.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "TRAILING_STOP_CREATED", "trailingStop", "trailing-stop-dlq");
        this.trailingStopService = trailingStopService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String username = (String) value.get("username");
        String stockCode = (String) value.get("stockCode");
        String trailingStopTypeStr = (String) value.get("trailingStopType");

        TrailingStopType trailingStopType;
        try {
            trailingStopType = TrailingStopType.valueOf(trailingStopTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid trailingStopType: {}", trailingStopTypeStr);
            return;
        }

        LeverageRatio leverageRatio;
        String leverageRatioStr = (String) value.get("leverageRatio");
        try {
            leverageRatio = leverageRatioStr == null
                    ? LeverageRatio.SPOT
                    : LeverageRatio.valueOf(leverageRatioStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid leverageRatio: {}", leverageRatioStr);
            return;
        }

        Integer orderQuantity = parseInt(value.get("orderQuantity"));
        Double stopPercent = parseDouble(value.get("stopPercent"));
        Integer basePrice = parseInt(value.get("basePrice"));

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip trailing stop for stockCode {}", stockCode);
            return;
        }

        log.info("Processing trailing stop username: {} for stock {}", username, stockCode);

        trailingStopService.registerTrailingStop(StockServerTrailingStopRequestEvent
                .of(username, stockCode, trailingStopType, orderQuantity, stopPercent, basePrice, leverageRatio));
    }

    private Integer parseInt(Object raw) {
        return raw == null ? null : Integer.parseInt(raw.toString());
    }

    private Double parseDouble(Object raw) {
        return raw == null ? null : Double.parseDouble(raw.toString());
    }
}
