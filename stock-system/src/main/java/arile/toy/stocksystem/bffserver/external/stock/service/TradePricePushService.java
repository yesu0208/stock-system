package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisTradePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradePricePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerRedisTradePriceRepository bffServerTradePriceRepository;

    public void push(String stockCode) {

        var tradePriceTickMessage = bffServerTradePriceRepository.findByStockCode(stockCode);

        messagingTemplate.convertAndSend(
                "/sub/stock/" + stockCode,
                tradePriceTickMessage);
    }
}
