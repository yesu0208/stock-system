package arile.toy.stocksystem.stockserver.otococancel.event.subscriber;

import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.otococancel.service.OtocoCancelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisOtocoCancelRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final OtocoCancelService otocoCancelService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisOtocoCancelRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            OtocoCancelService otocoCancelService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.otoco-cancel.prefix}") String prefix,
            @Value("${redis.streams.otoco-cancel.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "OTOCO_CANCEL_CREATED", "otocoCancel", "otoco-cancel-dlq");
        this.otocoCancelService = otocoCancelService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        Object otocoIdObj = value.get("otocoId");
        Long otocoId = null;
        if (otocoIdObj != null) {
            otocoId = otocoIdObj instanceof Number
                    ? ((Number) otocoIdObj).longValue()
                    : Long.parseLong(otocoIdObj.toString());
        }

        String stockCode = (String) value.get("stockCode");
        // 요청자: OTOCO 소유자 검증에 사용 (bff가 JWT에서 추출해 전달)
        String username = (String) value.get("username");

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip otoco cancel for stockCode {}", stockCode);
            return;
        }

        log.info("Processing otoco cancel otocoId: {} for stockCode {}", otocoId, stockCode);

        otocoCancelService.registerCancel(OtocoCancelRequestEvent.of(otocoId, stockCode, username));
    }
}
