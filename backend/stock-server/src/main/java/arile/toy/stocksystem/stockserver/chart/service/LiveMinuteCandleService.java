package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.stockserver.chart.event.MinuteCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.publisher.RedisMinuteCandleEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LiveMinuteCandleService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisMinuteCandleEventPublisher publisher;

    private final ConcurrentHashMap<String, MinuteCandleBuilder> builders = new ConcurrentHashMap<>();

    public void updateAndPublish(String stockCode, TradePriceTickMessage tick) {

        Integer curPrice = tick.curPrice();
        Integer volumeTick = tick.tradingVolumeTick();
        String tradeTime = tick.tradeTime();

        if (curPrice == null || tradeTime == null || tradeTime.length() < 4) {
            return;
        }

        int safeVolumeTick = volumeTick != null ? volumeTick : 0;
        String minuteKey = toMinuteKey(tradeTime);
        String today = LocalDate.now().format(DATE_FORMAT);

        MinuteCandleBuilder builder = builders.compute(stockCode, (code, existing) -> {
            if (existing == null || !existing.minuteKey.equals(minuteKey)) {
                return new MinuteCandleBuilder(today, minuteKey, curPrice, safeVolumeTick);
            }
            existing.accumulate(curPrice, safeVolumeTick);
            return existing;
        });

        publisher.publish(MinuteCandleUpdateEvent.of(stockCode, builder.toCandle()));
    }

    private String toMinuteKey(String tradeTime) {
        return tradeTime.substring(0, 4) + "00";
    }

    private static class MinuteCandleBuilder {
        private final String date;
        private final String minuteKey;
        private long open, high, low, close, volume;

        MinuteCandleBuilder(String date, String minuteKey, int firstPrice, int firstVolumeTick) {
            this.date = date;
            this.minuteKey = minuteKey;
            this.open = this.high = this.low = this.close = firstPrice;
            this.volume = Math.max(firstVolumeTick, 0);
        }

        void accumulate(int price, int volumeTick) {
            close = price;
            high = Math.max(high, price);
            low = Math.min(low, price);
            volume += Math.max(volumeTick, 0);
        }

        MinuteCandle toCandle() {
            return new MinuteCandle(date, minuteKey, open, high, low, close, volume);
        }
    }
}
