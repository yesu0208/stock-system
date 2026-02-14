package arile.toy.stocksystem.stockserver.trading.event.subscriber;

import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;
import arile.toy.stocksystem.stockserver.trading.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.trading.service.AutoOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisAutoOrderRequestEventConsumer {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final AutoOrderService autoOrderService;
    private final StockServerMarketPhaseRegistry registry;

    @Value("${redis.streams.auto-order.prefix}")
    private String prefix;

    @Value("${redis.streams.auto-order.consumer-group}")
    private String group;

    @Value("${server.shard-index}")
    private int shardIndex;

    private final String consumerName =
            "stock-server" + UUID.randomUUID();

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        String streamKey = prefix + "-" + shardIndex;

        List<MapRecord<String, Object, Object>> records =
                streamRedisTemplate.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty()
                                .count(10)
                                .block(Duration.ofSeconds(5)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handle(record);
                streamRedisTemplate.opsForStream()
                        .acknowledge(streamKey, group, record.getId());
            } catch (Exception e) {
                log.error("Failed to process {}", record.getId(), e);
            }
        }
        // Todo: 재처리 과정
    }

    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String type = (String) value.get("type");
        if (!"AUTO_ORDER_CREATED".equals(type)) {
            return;
        }

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
                .of(username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity));
    }
}
