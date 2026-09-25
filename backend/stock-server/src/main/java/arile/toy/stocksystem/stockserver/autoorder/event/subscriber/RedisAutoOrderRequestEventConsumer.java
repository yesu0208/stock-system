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

        AutoOrderType autoOrderType;
        try {
            autoOrderType = AutoOrderType.valueOf(autoOrderTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
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

        Object rawOrderPrice = value.get("orderPrice");
        Integer orderPrice = null;

        if (rawOrderPrice != null) {
            orderPrice = Integer.parseInt(rawOrderPrice.toString());
        }

        Object rawTriggerPrice = value.get("triggerPrice");
        Integer triggerPrice = null;

        if (rawTriggerPrice != null) {
            triggerPrice = Integer.parseInt(rawTriggerPrice.toString());
        }


        Object rawOrderQuantity = value.get("orderQuantity");
        Integer orderQuantity = null;

        if (rawOrderQuantity != null) {
            orderQuantity = Integer.parseInt(rawOrderQuantity.toString());
        }

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip auto order for stockCode {}", stockCode);
            return;
        }

        log.info("Processing order username: {} for stock {}", username, stockCode);

        autoOrderService.registerAutoOrder(StockServerAutoOrderRequestEvent
                .of(username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity, leverageRatio));
    }
}
