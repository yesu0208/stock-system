package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisBidAskPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BidAskPricePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerRedisBidAskPriceRepository bffServerRedisBidAskPriceRepository;

    public void push(String stockCode) {

        var bidAskPriceTickMessage = bffServerRedisBidAskPriceRepository.findByStockCode(stockCode);

        messagingTemplate.convertAndSend(
                "/sub/stock/" + stockCode,
                bidAskPriceTickMessage);
    }
}
