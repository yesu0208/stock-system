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
        } catch (IllegalArgumentException | NullPointerException e) {
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

        // 잘못된 모드 값은 재시도해도 같은 결과이므로 예외(재시도·DLQ) 대신 로그만 남기고 건너뜀
        OtocoExitMode tpMode = parseExitMode(value.get("tpMode"));
        OtocoExitMode slMode = parseExitMode(value.get("slMode"));
        if (tpMode == null || slMode == null) {
            log.error("Invalid exit mode: tpMode={}, slMode={}", value.get("tpMode"), value.get("slMode"));
            return;
        }

        Integer orderQuantity = parseInt(value.get("orderQuantity"));
        Integer entryTriggerPrice = parseInt(value.get("entryTriggerPrice"));
        Integer tpPrice = parseInt(value.get("tpPrice"));
        Double tpPct = parseDouble(value.get("tpPct"));
        Integer slPrice = parseInt(value.get("slPrice"));
        Double slPct = parseDouble(value.get("slPct"));

        // 수량·진입가는 예약 금액 계산에 필요하므로, 없거나 잘못된 값이면 재시도하지 않고 건너뜀
        if (orderQuantity == null || entryTriggerPrice == null) {
            log.error("Invalid otoco values. orderQuantity: {}, entryTriggerPrice: {}",
                    value.get("orderQuantity"), value.get("entryTriggerPrice"));
            return;
        }

        // 모드에 필요한 값이 없으면 익절·손절가 계산 중 NPE가 나므로 미리 걸러냄
        if (!hasExitValue(tpMode, tpPrice, tpPct) || !hasExitValue(slMode, slPrice, slPct)) {
            log.error("Missing exit value: tpMode={}, tpPrice={}, tpPct={}, slMode={}, slPrice={}, slPct={}",
                    tpMode, tpPrice, tpPct, slMode, slPrice, slPct);
            return;
        }

        if (registry.isClosed(stockCode)) {
            log.info("Market closed. Skip otoco for stockCode {}", stockCode);
            return;
        }

        log.info("Processing otoco username: {} for stock {}", username, stockCode);

        otocoService.registerOtoco(new StockServerOtocoRequestEvent(
                username, stockCode, entryDirection, orderQuantity, entryTriggerPrice,
                tpMode, tpPrice, tpPct, slMode, slPrice, slPct, leverageRatio));
    }

    private OtocoExitMode parseExitMode(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return OtocoExitMode.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean hasExitValue(OtocoExitMode mode, Integer price, Double pct) {
        return mode == OtocoExitMode.PRICE ? price != null : pct != null;
    }

    private Integer parseInt(Object raw) {
        if (raw == null || "null".equals(raw.toString())) return null;
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(Object raw) {
        if (raw == null || "null".equals(raw.toString())) return null;
        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
