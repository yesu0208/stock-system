package arile.toy.stocksystem.accountserver.trade.event.subscriber;

import arile.toy.stocksystem.accountserver.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.trade.service.TradeExecutionApplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTradeExecutedEventConsumer extends AbstractRedisStreamConsumer {

    private final TradeExecutionApplyService tradeExecutionApplyService;
    private final ObjectMapper objectMapper;

    public RedisTradeExecutedEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            TradeExecutionApplyService tradeExecutionApplyService,
            ObjectMapper objectMapper,
            @Value("${redis.streams.trade-executed.key}") String streamKey,
            @Value("${redis.streams.trade-executed.consumer-group}") String group
    ) {
        super(streamRedisTemplate, streamKey, group);
        this.tradeExecutionApplyService = tradeExecutionApplyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected String eventType() {
        return "TRADE_EXECUTED";
    }

    @Override
    protected String processedKeyPrefix() {
        return "processed:tradeExecuted:";
    }

    @Override
    protected String dlqStreamKey() {
        return "trade-executed-dlq";
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        String payload = (String) record.getValue().get("payload");

        try {
            TradeExecutedEvent event = objectMapper.readValue(payload, TradeExecutedEvent.class);
            tradeExecutionApplyService.apply(event);
        } catch (Exception e) {
            log.error("Failed to parse/apply TradeExecutedEvent. payload={}", payload, e);
            throw new IllegalStateException(e);
        }
    }
}
