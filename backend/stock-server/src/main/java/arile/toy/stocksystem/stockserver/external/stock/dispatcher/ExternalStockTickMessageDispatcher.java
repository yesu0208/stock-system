package arile.toy.stocksystem.stockserver.external.stock.dispatcher;

import arile.toy.stocksystem.stockserver.external.stock.handler.BidAskPriceTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StateTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StockSummaryTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.TradePriceTickMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalStockTickMessageDispatcher {

    private final StateTickMessageHandler stateTickMessageHandler;
    private final TradePriceTickMessageHandler tradePriceTickMessageHandler;
    private final BidAskPriceTickMessageHandler bidAskPriceTickMessageHandler;
    private final StockSummaryTickMessageHandler stockSummaryTickMessageHandler;

    public void dispatch(String message) {

        if (!message.contains("|")) {
            stateTickMessageHandler.handle(message);
            return;
        }

        String[] parts = message.split("\\|", 4);
        String trId = parts[1];

        if (trId.equals("H0STCNT0")) {
            tradePriceTickMessageHandler.handle(message);
            stockSummaryTickMessageHandler.handle(message);
        } else {
            bidAskPriceTickMessageHandler.handle(message);
        }
    }
}
