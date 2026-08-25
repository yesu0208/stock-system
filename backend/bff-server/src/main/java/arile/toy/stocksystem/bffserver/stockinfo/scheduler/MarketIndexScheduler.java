package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketIndexScheduler {

    private static final String DESTINATION = "/sub/market/main";

    private final NaverStockCrawlerClient naverStockCrawlerClient;
    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicReference<MarketMainResponse> latestSnapshot = new AtomicReference<>();

    @Scheduled(fixedRate = 10_000)
    public void broadcastMarketIndices() {
        try {
            MarketMainResponse response = naverStockCrawlerClient.getMarketIndices();
            latestSnapshot.set(response);
            messagingTemplate.convertAndSend(DESTINATION, response);
        } catch (Exception e) {
            log.error("지수 브로드캐스트 실패", e);
        }
    }

    public MarketMainResponse getLatestSnapshot() {
        return latestSnapshot.get();
    }
}
