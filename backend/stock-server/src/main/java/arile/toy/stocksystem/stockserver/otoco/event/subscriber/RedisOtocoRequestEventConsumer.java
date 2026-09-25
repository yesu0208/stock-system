package arile.toy.stocksystem.stockserver.otoco.event.subscriber;

import arile.toy.stocksystem.stockserver.common.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhaseRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoExitMode;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class RedisOtocoRequestEventConsumer extends AbstractRedisStreamConsumer {

    private final OtocoService otocoService;
    private final StockServerMarketPhaseRegistry registry;

    public RedisOtocoRequestEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            OtocoService otocoService,
            StockServerMarketPhaseRegistry registry,
            @Value("${redis.streams.otoco.prefix}") String prefix,
            @Value("${redis.streams.otoco.consumer-group}") String group,
            @Value("${server.group}") String stockGroup) {
        super(streamRedisTemplate, prefix + "-" + stockGroup, group,
                "OTOCO_CREATED", "otoco", "otoco-dlq");
        this.otocoService = otocoService;
        this.registry = registry;
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();

        String username = (String) value.get("username");
        String stockCode = (String) value.get("stockCode");
        String entryDirectionStr = (String) value.get("entryDirection");

        OtocoEntryDirection entryDirection;
        try {
            entryDirection = OtocoEntryDirection.valueOf(entryDirectionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid entryDirection: {}", entryDirectionStr);
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

        OtocoExitMode tpMode = OtocoExitMode.valueOf(((String) value.get("tpMode")).toUpperCase());
        OtocoExitMode slMode = OtocoExitMode.valueOf(((String) value.get("slMode")).toUpperCase());

        Integer orderQuantity = parseInt(value.get("orderQuantity"));
        Integer entryTriggerPrice = parseInt(value.get("entryTriggerPrice"));
        Integer tpPrice = parseInt(value.get("tpPrice"));
        Double tpPct = parseDouble(value.get("tpPct"));
        Integer slPrice = parseInt(value.get("slPrice"));
        Double slPct = parseDouble(value.get("slPct"));

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip otoco for stockCode {}", stockCode);
            return;
        }

        log.info("Processing otoco username: {} for stock {}", username, stockCode);

        otocoService.registerOtoco(new StockServerOtocoRequestEvent(
                username, stockCode, entryDirection, orderQuantity, entryTriggerPrice,
                tpMode, tpPrice, tpPct, slMode, slPrice, slPct, leverageRatio));
    }

    private Integer parseInt(Object raw) {
        return raw == null || "null".equals(raw.toString()) ? null : Integer.parseInt(raw.toString());
    }

    private Double parseDouble(Object raw) {
        return raw == null || "null".equals(raw.toString()) ? null : Double.parseDouble(raw.toString());
    }
}
