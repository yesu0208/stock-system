package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.TradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradePricePushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(TradePriceTickMessage tradePriceTickMessage) {

        messagingTemplate.convertAndSend(
                "/sub/stock/" + tradePriceTickMessage.stockCode(),
                tradePriceTickMessage);
    }
}
