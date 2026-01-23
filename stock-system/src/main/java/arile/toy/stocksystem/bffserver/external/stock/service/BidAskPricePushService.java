package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.BidAskPriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BidAskPricePushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(BidAskPriceTickMessage bidAskPriceTickMessage) {

        messagingTemplate.convertAndSend(
                "/sub/stock/" + bidAskPriceTickMessage.stockCode(),
                bidAskPriceTickMessage);
    }
}
