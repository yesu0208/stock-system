package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.checker.MarketTimeChecker;
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
    private final MarketTimeChecker marketTimeChecker;

    public void closeMarketAfterClosingCall(String stockCode, String tradeTime) {
        LocalTime time = LocalTime.parse(tradeTime, DateTimeFormatter.ofPattern("HHmmss"));

        LocalTime windowStart = LocalTime.of(15, 29, 50);
        LocalTime windowEnd = LocalTime.of(15, 37, 50);

        if (!time.isBefore(windowStart) && time.isBefore(windowEnd)) {
            updateMarketPhase(stockCode, StockServerMarketPhase.CLOSED);
        }
    }

    public void setScheduledMarkets() {
        StockServerMarketPhase currentPhase = marketTimeChecker.resolvePhase();
        if (currentPhase.isOrderable()) {
            stockProperties.getOpen()
                    .forEach(stockCode -> updateMarketPhase(stockCode, currentPhase));
        } else {
            closeScheduledOpenMarkets();
        }
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

    public void openMorningCall() {
        stockProperties.getOpen()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.MORNING_CALL));
    }

    public void openRegularMarket() {
        stockProperties.getOpen()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.OPEN));
    }

    public void openClosingCall() {
        stockProperties.getOpen()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.CLOSING_CALL));
    }

    public void openAfterMarket() {
        stockProperties.getOpen()
                .forEach(stockCode -> updateMarketPhase(stockCode, StockServerMarketPhase.AFTER));
    }

    public void updateMarketPhase(String stockCode, StockServerMarketPhase phase) {
        if (registry.getPhase(stockCode) == phase) {
            return;
        }

        registry.setPhase(stockCode, phase);
        marketPhasePublisher.publish(stockCode, phase);
        log.info("Market {} for stock {}.", phase.name(), stockCode);
    }
}
