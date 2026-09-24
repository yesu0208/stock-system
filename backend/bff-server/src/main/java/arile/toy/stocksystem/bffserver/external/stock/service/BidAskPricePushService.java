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

        // 호가가 아직 저장되지 않았거나 만료되었으면 보내지 않음 (null 페이로드는 전송 시 예외 발생)
        if (bidAskPriceTickMessage == null) return;

        messagingTemplate.convertAndSend(
                "/sub/stock/" + stockCode,
                bidAskPriceTickMessage);
    }
}
