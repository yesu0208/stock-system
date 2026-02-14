package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketPhaseService {

    private final StockServerMarketPhaseRegistry registry;
    private final ExternalStockProperties stockProperties;
    private final MarketPhasePublisher marketPhasePublisher;

    public void closeMarketAfterClosingCall(String stockCode, String tradeTime) {

        LocalTime time = LocalTime.parse(
                tradeTime,
                DateTimeFormatter.ofPattern("HHmmss")
        );

        if (time.isAfter(LocalTime.of(15, 29, 50))) {
            updateMarketPhase(stockCode, StockServerMarketPhase.CLOSED);
        }
    }

    public void setScheduledMarkets() {
        openScheduledOpenMarkets();
        closeScheduledCloseMarkets();
    }

    public void closeAllMarkets() {
        closeScheduledOpenMarkets();
        closeScheduledCloseMarkets();
    }

    public void openScheduledOpenMarkets() {
        stockProperties.getOpen()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.OPEN));
    }

    public void closeScheduledOpenMarkets() {
        stockProperties.getOpen()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.CLOSED));
    }

    public void closeScheduledCloseMarkets() {
        stockProperties.getClose()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.CLOSED));
    }

    public void updateMarketPhase(String stockCode, StockServerMarketPhase phase) {

        if ((phase == StockServerMarketPhase.OPEN && registry.isOpened(stockCode)) ||
                (phase == StockServerMarketPhase.CLOSED && registry.isClosed(stockCode))) {
            return;
        }

        registry.setPhase(stockCode, phase);

        marketPhasePublisher.publish(stockCode, phase);

        log.info("Market {} for stock {}.", phase.name(), stockCode);
    }
}
