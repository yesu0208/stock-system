package arile.toy.stocksystem.bffserver.chart.service;

import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandleTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LiveMinuteCandleService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SimpMessagingTemplate messagingTemplate;

    // 종목코드 -> 현재 만들어지고 있는 분봉
    private final ConcurrentHashMap<String, MinuteCandleBuilder> builders = new ConcurrentHashMap<>();

    public void updateAndPush(String stockCode, BffServerTradePriceTickMessage tick) {

        Integer curPrice = tick.curPrice();
        Integer volumeTick = tick.tradingVolumeTick();
        String tradeTime = tick.tradeTime();

        if (curPrice == null || tradeTime == null || tradeTime.length() < 4) {
            return; // 필수 데이터 없으면 스킵
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

        messagingTemplate.convertAndSend(
                "/sub/stock/" + stockCode,
                MinuteCandleTickMessage.of(stockCode, builder.toCandle())
        );
    }

    // 구독 완전 해제 시 메모리 정리용
    public void clear(String stockCode) {
        builders.remove(stockCode);
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
