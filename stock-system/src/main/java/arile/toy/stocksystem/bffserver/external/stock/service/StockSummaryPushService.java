package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisStockSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSummaryPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerRedisStockSummaryRepository bffServerStockSummaryRepository;

    public void push(String stockCode) {

       var stockSummaryTickMessage = bffServerStockSummaryRepository.findByStockCode(stockCode);

       messagingTemplate.convertAndSend(
               "/sub/stock/summary",
               BffServerStockSummaryClientTickMessage.fromBiffServerStockSummaryTickMessage(stockSummaryTickMessage));
    }

    @Scheduled(fixedDelay = 1000)
    public void pushAll() {

        List<BffServerStockSummaryTickMessage> bffServerStockSummaryTickMessages =
                bffServerStockSummaryRepository.findAll();

        List<BffServerStockSummaryClientTickMessage> clientMessages =
                bffServerStockSummaryTickMessages.stream()
                        .map(BffServerStockSummaryClientTickMessage::fromBiffServerStockSummaryTickMessage)
                        .toList();

        messagingTemplate.convertAndSend(
                "/sub/stock/summary", clientMessages);
    }
}
