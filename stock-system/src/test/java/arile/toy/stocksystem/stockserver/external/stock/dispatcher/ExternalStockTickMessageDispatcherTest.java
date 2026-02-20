package arile.toy.stocksystem.stockserver.external.stock.dispatcher;

import arile.toy.stocksystem.stockserver.external.stock.handler.BidAskPriceTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StateTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StockSummaryTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.TradePriceTickMessageHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExternalStockTickMessageDispatcherTest {

    @Mock
    private StateTickMessageHandler stateTickMessageHandler;

    @Mock
    private TradePriceTickMessageHandler tradePriceTickMessageHandler;

    @Mock
    private BidAskPriceTickMessageHandler bidAskPriceTickMessageHandler;

    @Mock
    private StockSummaryTickMessageHandler stockSummaryTickMessageHandler;

    @InjectMocks
    private ExternalStockTickMessageDispatcher dispatcher;

    @Test
    @DisplayName("구분자 없는 메시지는 StateTickHandler에서 처리된다")
    void givenMessageWithoutDelimiter_whenDispatch_thenHandleByStateTickHandler() {
        // given
        String message = "SOME_STATE_MESSAGE";

        // when
        dispatcher.dispatch(message);

        // then
        verify(stateTickMessageHandler).handle(message);
        verifyNoInteractions(tradePriceTickMessageHandler, bidAskPriceTickMessageHandler, stockSummaryTickMessageHandler);
    }

    @Test
    @DisplayName("TradePrice 메시지는 Trade와 Summary 핸들러에서 처리된다")
    void givenTradePriceMessage_whenDispatch_thenHandleByTradeAndSummaryHandlers() {
        // given
        String message = "prefix|H0STCNT0|data|more";

        // when
        dispatcher.dispatch(message);

        // then
        verify(tradePriceTickMessageHandler).handle(message);
        verify(stockSummaryTickMessageHandler).handle(message);
        verifyNoInteractions(stateTickMessageHandler, bidAskPriceTickMessageHandler);
    }

    @Test
    @DisplayName("BidAsk 메시지는 BidAskPriceTickHandler에서 처리된다")
    void givenBidAskMessage_whenDispatch_thenHandleByBidAskHandler() {
        // given
        String message = "prefix|H0STPRC1|data|more";

        // when
        dispatcher.dispatch(message);

        // then
        verify(bidAskPriceTickMessageHandler).handle(message);
        verifyNoInteractions(stateTickMessageHandler, tradePriceTickMessageHandler, stockSummaryTickMessageHandler);
    }
}
