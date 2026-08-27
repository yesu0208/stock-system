package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.event.DailyCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.publisher.RedisDailyCandleEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class LiveDailyCandleService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisDailyCandleEventPublisher publisher;

    public void buildAndPublish(String stockCode, TradePriceTickMessage tick) {

        Integer startPrice = tick.startPrice();
        Integer highPrice = tick.highPrice();
        Integer lowPrice = tick.lowPrice();
        Integer curPrice = tick.curPrice();
        Integer totalVolume = tick.totalTradingVolume();

        if (startPrice == null || highPrice == null || lowPrice == null
                || curPrice == null || totalVolume == null) {
            return;
        }

        CandleData todayCandle = new CandleData(
                LocalDate.now().format(DATE_FORMAT),
                startPrice, highPrice, lowPrice, curPrice, totalVolume
        );

        publisher.publish(DailyCandleUpdateEvent.of(stockCode, todayCandle));
    }
}
