package arile.toy.stocksystem.stockserver.autoorder.event.subscriber;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisAutoOrderRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final AutoOrderService autoOrderService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisAutoOrderRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            AutoOrderService autoOrderService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.auto-order.prefix}") String prefix,
            @Value("${redis.streams.auto-order.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "AUTO_ORDER_CREATED", "autoOrder", "auto-order-dlq");
        this.autoOrderService = autoOrderService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String username = (String) value.get("username");
        String stockCode = (String) value.get("stockCode");
        String autoOrderTypeStr = (String) value.get("autoOrderType");

        // 잘못된 값은 재시도해도 같은 결과이므로 예외(재시도·DLQ) 대신 로그만 남기고 건너뜀
        AutoOrderType autoOrderType;
        try {
            autoOrderType = AutoOrderType.valueOf(autoOrderTypeStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid orderType: {}", autoOrderTypeStr);
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

        // 가격·수량이 없거나 숫자가 아니면 예약 금액 계산에서 매번 실패하므로
        // 예외(재시도·DLQ) 대신 로그만 남기고 건너뜀
        Integer orderPrice = parseInt(value.get("orderPrice"));
        Integer triggerPrice = parseInt(value.get("triggerPrice"));
        Integer orderQuantity = parseInt(value.get("orderQuantity"));
        if (orderPrice == null || triggerPrice == null || orderQuantity == null) {
            log.error("Invalid auto order values: orderPrice={}, triggerPrice={}, orderQuantity={}",
                    value.get("orderPrice"), value.get("triggerPrice"), value.get("orderQuantity"));
            return;
        }

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip auto order for stockCode {}", stockCode);
            return;
        }

        log.info("Processing order username: {} for stock {}", username, stockCode);

        autoOrderService.registerAutoOrder(StockServerAutoOrderRequestEvent
                .of(username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity, leverageRatio));
    }

    private Integer parseInt(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
