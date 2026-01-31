package arile.toy.stocksystem.stockserver.trading.event.subscriber;

import arile.toy.stocksystem.stockserver.trading.dto.order.OrderType;
import arile.toy.stocksystem.stockserver.trading.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.trading.service.OrderService;
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
public class RedisOrderRequestEventConsumer {

    private final RedisTemplate<String, Object> streamRedisTemplate;
    private final OrderService orderService;

    @Value("${redis.streams.order.prefix}")
    private String prefix;

    @Value("${redis.streams.order.consumer-group}")
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
        if (!"ORDER_CREATED".equals(type)) {
            return;
        }

        String username = (String) value.get("username");
        String stockCode = (String) value.get("stockCode");
        String orderTypeStr = (String) value.get("orderType");

        OrderType orderType;
        try {
            orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid orderType: {}", orderTypeStr);
            return;
        }

        Object rawOrderPrice = value.get("orderPrice");
        Integer orderPrice = null;

        if (rawOrderPrice != null) {
            orderPrice = Integer.parseInt(rawOrderPrice.toString());
        }

        Object rawOrderQuantity = value.get("orderQuantity");
        Integer orderQuantity = null;

        if (rawOrderQuantity != null) {
            orderQuantity = Integer.parseInt(rawOrderQuantity.toString());
        }

        log.info("Processing order username: {} for stock {}", username, stockCode);

        orderService.registerOrder(StockServerOrderRequestEvent
                .of(username, stockCode, orderType, orderPrice, orderQuantity));
    }
}
