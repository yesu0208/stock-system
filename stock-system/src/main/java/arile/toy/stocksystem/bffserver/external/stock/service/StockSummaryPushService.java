package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.StockSummaryTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockSummaryPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(StockSummaryTickMessage stockSummaryTickMessage) {

       messagingTemplate.convertAndSend(
               "/sub/stock/summary", stockSummaryTickMessage);
    }
}
