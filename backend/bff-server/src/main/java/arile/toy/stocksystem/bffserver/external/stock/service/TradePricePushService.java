package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.chart.service.LiveDailyCandleService;
import arile.toy.stocksystem.bffserver.chart.service.LiveMinuteCandleService;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisTradePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradePricePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerRedisTradePriceRepository bffServerTradePriceRepository;
    private final LiveDailyCandleService liveDailyCandleService;
    private final LiveMinuteCandleService liveMinuteCandleService;

    public void push(String stockCode) {

        BffServerTradePriceTickMessage tradePriceTickMessage =
                bffServerTradePriceRepository.findByStockCode(stockCode);

        if (tradePriceTickMessage == null) return;

        messagingTemplate.convertAndSend(
                "/sub/stock/" + stockCode,
                BffServerTradePriceClientTickMessage.fromTickMessage(tradePriceTickMessage));

        liveDailyCandleService.buildAndPush(stockCode, tradePriceTickMessage);
        liveMinuteCandleService.updateAndPush(stockCode, tradePriceTickMessage);
    }
}
