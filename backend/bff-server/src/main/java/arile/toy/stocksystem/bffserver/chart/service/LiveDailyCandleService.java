package arile.toy.stocksystem.bffserver.chart.service;

import arile.toy.stocksystem.bffserver.chart.dto.CandleData;
import arile.toy.stocksystem.bffserver.chart.dto.DailyCandleTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class LiveDailyCandleService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SimpMessagingTemplate messagingTemplate;

    public void buildAndPush(String stockCode, BffServerTradePriceTickMessage tick) {

        Integer startPrice = tick.startPrice();
        Integer highPrice = tick.highPrice();
        Integer lowPrice = tick.lowPrice();
        Integer curPrice = tick.curPrice();
        Integer totalVolume = tick.totalTradingVolume();

        if (startPrice == null || highPrice == null || lowPrice == null
                || curPrice == null || totalVolume == null) {
            return; // 데이터 불완전하면 스킵
        }

        CandleData todayCandle = new CandleData(
                LocalDate.now().format(DATE_FORMAT),
                startPrice,
                highPrice,
                lowPrice,
                curPrice,
                totalVolume
        );

        messagingTemplate.convertAndSend(
                "/sub/stock/" + stockCode,
                DailyCandleTickMessage.of(stockCode, todayCandle)
        );
    }
}
