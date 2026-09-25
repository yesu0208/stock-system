package arile.toy.stocksystem.stockserver.external.stock.dispatcher;

import arile.toy.stocksystem.stockserver.external.stock.handler.BidAskPriceTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StateTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StockSummaryTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.TradePriceTickMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalStockTickMessageDispatcher {

    private final StateTickMessageHandler stateTickMessageHandler;
    private final TradePriceTickMessageHandler tradePriceTickMessageHandler;
    private final BidAskPriceTickMessageHandler bidAskPriceTickMessageHandler;
    private final StockSummaryTickMessageHandler stockSummaryTickMessageHandler;

    public void dispatch(String message) {

        try {
            if (!message.contains("|")) {
                runSafely("state", () -> stateTickMessageHandler.handle(message));
                return;
            }

            String[] parts = message.split("\\|", 4);
            String trId = parts[1];

            if (trId.equals("H0STCNT0")) {
                runSafely("tradePrice", () -> tradePriceTickMessageHandler.handle(message));
                runSafely("stockSummary", () -> stockSummaryTickMessageHandler.handle(message));
            } else {
                runSafely("bidAskPrice", () -> bidAskPriceTickMessageHandler.handle(message));
            }
        } catch (Exception e) {
            log.error("External stock tick message dispatch failed. message={}", message, e);
        }
    }

    private void runSafely(String handlerName, Runnable handler) {
        try {
            handler.run();
        } catch (Exception e) {
            log.error("External stock tick handler failed. handler={}", handlerName, e);
        }
    }
}
