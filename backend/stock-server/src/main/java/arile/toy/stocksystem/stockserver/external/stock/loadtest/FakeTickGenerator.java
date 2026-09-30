package arile.toy.stocksystem.stockserver.external.stock.loadtest;

import arile.toy.stocksystem.stockserver.external.stock.dispatcher.ExternalStockTickMessageDispatcher;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(name = "loadtest.fake-tick.enabled", havingValue = "true")
public class FakeTickGenerator {

    static final String TRADE_PRICE_TR_ID = "H0STCNT0";
    static final String SELL_EXECUTED = "5";
    static final String SAFE_TRADE_TIME = "120000";

    private static final int FIELD_COUNT = 47;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final LocalTime CLOSING_CALL_START = LocalTime.of(15, 29, 50);
    private static final LocalTime CLOSING_CALL_END = LocalTime.of(15, 37, 50);

    private final ExternalStockTickMessageDispatcher dispatcher;
    private final List<String> stockCodes;
    private final int minPrice;
    private final int maxPrice;
    private final int volume;
    private final Clock clock;

    private final Map<String, AtomicLong> accumulatedVolumes = new ConcurrentHashMap<>();

    @Autowired
    public FakeTickGenerator(ExternalStockTickMessageDispatcher dispatcher,
                             @Value("${loadtest.fake-tick.stock-codes}") String stockCodes,
                             @Value("${loadtest.fake-tick.min-price:990}") int minPrice,
                             @Value("${loadtest.fake-tick.max-price:1000}") int maxPrice,
                             @Value("${loadtest.fake-tick.volume:20}") int volume) {
        this(dispatcher, parseStockCodes(stockCodes), minPrice, maxPrice, volume, Clock.systemDefaultZone());
    }

    FakeTickGenerator(ExternalStockTickMessageDispatcher dispatcher, List<String> stockCodes,
                      int minPrice, int maxPrice, int volume, Clock clock) {
        this.dispatcher = dispatcher;
        this.stockCodes = stockCodes;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.volume = volume;
        this.clock = clock;
    }

    @PostConstruct
    void warn() {
        log.warn("[LOADTEST] loadtest.fake-tick.enabled=true: 가짜 체결가 틱을 생성합니다. stockCodes={}, price={}~{}, volume={}. 운영 환경이면 즉시 끄세요.",
                stockCodes, minPrice, maxPrice, volume);
    }

    @Scheduled(fixedRateString = "${loadtest.fake-tick.interval-ms:100}")
    public void generate() {
        String tradeTime = tradeTime();
        for (String stockCode : stockCodes) {
            int price = ThreadLocalRandom.current().nextInt(minPrice, maxPrice + 1);
            dispatcher.dispatch(buildMessage(stockCode, price, tradeTime));
        }
    }

    String buildMessage(String stockCode, int price, String tradeTime) {
        long accumulated = accumulatedVolumes
                .computeIfAbsent(stockCode, code -> new AtomicLong())
                .addAndGet(volume);

        String[] fields = new String[FIELD_COUNT];
        Arrays.fill(fields, "0");
        fields[0] = stockCode;
        fields[1] = tradeTime;
        fields[2] = String.valueOf(price);
        fields[4] = "0";
        fields[7] = String.valueOf(price);
        fields[8] = String.valueOf(price);
        fields[9] = String.valueOf(price);
        fields[12] = String.valueOf(volume);
        fields[13] = String.valueOf(accumulated);
        fields[14] = String.valueOf(accumulated * price);
        fields[19] = String.valueOf(accumulated);
        fields[20] = "0";
        fields[21] = SELL_EXECUTED;
        fields[41] = "0";

        return "0|" + TRADE_PRICE_TR_ID + "|1|" + String.join("^", fields);
    }

    String tradeTime() {
        LocalTime now = LocalTime.now(clock);
        if (!now.isBefore(CLOSING_CALL_START) && now.isBefore(CLOSING_CALL_END)) {
            return SAFE_TRADE_TIME;
        }
        return now.format(TIME_FORMAT);
    }

    private static List<String> parseStockCodes(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
    }
}
