package arile.toy.stocksystem.stockserver.order.event.subscriber;

import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisOrderRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final OrderService orderService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisOrderRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            OrderService orderService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.order.prefix}") String prefix,
            @Value("${redis.streams.order.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "ORDER_CREATED", "order", "order-dlq");
        this.orderService = orderService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String username = (String) value.get("username");
        String stockCode = (String) value.get("stockCode");
        String orderTypeStr = (String) value.get("orderType");

        // 잘못된 값은 재시도해도 같은 결과이므로 예외(재시도·DLQ) 대신 로그만 남기고 건너뜀
        OrderType orderType;
        try {
            orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid orderType: {}", orderTypeStr);
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

        // orderExecutionType 파싱 — 없으면(구버전 프론트 호환) LIMIT으로 간주
        OrderExecutionType orderExecutionType;
        String orderExecutionTypeStr = (String) value.get("orderExecutionType");
        try {
            orderExecutionType = orderExecutionTypeStr == null
                    ? OrderExecutionType.LIMIT
                    : OrderExecutionType.valueOf(orderExecutionTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid orderExecutionType: {}", orderExecutionTypeStr);
            return;
        }

        // 가격·수량이 없거나 숫자가 아니면 예약 금액 계산에서 매번 실패하므로
        // 예외(재시도·DLQ) 대신 로그만 남기고 건너뜀 (bff에서 가격·수량은 필수값)
        Integer orderPrice = parseInt(value.get("orderPrice"));
        Integer orderQuantity = parseInt(value.get("orderQuantity"));
        if (orderPrice == null || orderQuantity == null) {
            log.error("Invalid order values: orderPrice={}, orderQuantity={}",
                    value.get("orderPrice"), value.get("orderQuantity"));
            return;
        }

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip order for stockCode {}", stockCode);
            return;
        }
        log.info("Processing order username: {} for stock {}", username, stockCode);

        orderService.registerOrder(StockServerOrderRequestEvent
                .of(username, stockCode, orderType, orderPrice, orderQuantity, leverageRatio, orderExecutionType), false);
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
