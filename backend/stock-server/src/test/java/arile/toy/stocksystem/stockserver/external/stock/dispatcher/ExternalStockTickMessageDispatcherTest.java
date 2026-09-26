package arile.toy.stocksystem.stockserver.external.stock.dispatcher;

import arile.toy.stocksystem.stockserver.external.stock.handler.BidAskPriceTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StateTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.StockSummaryTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.handler.TradePriceTickMessageHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@DisplayName("[Dispatcher] 외부 시세 메시지 분배 테스트")
@ExtendWith(MockitoExtension.class)
class ExternalStockTickMessageDispatcherTest {

    @InjectMocks private ExternalStockTickMessageDispatcher sut;

    @Mock private StateTickMessageHandler stateTickMessageHandler;
    @Mock private TradePriceTickMessageHandler tradePriceTickMessageHandler;
    @Mock private BidAskPriceTickMessageHandler bidAskPriceTickMessageHandler;
    @Mock private StockSummaryTickMessageHandler stockSummaryTickMessageHandler;

    @DisplayName("구분자가 없는 메시지는 상태 핸들러로 보낸다")
    @Test
    void givenJson_whenDispatching_thenState() {
        sut.dispatch("{\"header\":{\"tr_id\":\"PINGPONG\"}}");

        then(stateTickMessageHandler).should().handle("{\"header\":{\"tr_id\":\"PINGPONG\"}}");
        then(tradePriceTickMessageHandler).shouldHaveNoInteractions();
    }

    @DisplayName("체결가(H0STCNT0)는 체결가·요약 핸들러로, 그 외는 호가 핸들러로 보낸다")
    @Test
    void givenRealtime_whenDispatching_thenRoutesByTrId() {
        sut.dispatch("0|H0STCNT0|1|x");
        sut.dispatch("0|H0STASP0|1|y");

        then(tradePriceTickMessageHandler).should().handle("0|H0STCNT0|1|x");
        then(stockSummaryTickMessageHandler).should().handle("0|H0STCNT0|1|x");
        then(bidAskPriceTickMessageHandler).should().handle("0|H0STASP0|1|y");
    }

    @DisplayName("체결가 핸들러가 실패해도 요약 핸들러는 실행하고, 예외는 밖으로 던지지 않는다")
    @Test
    void givenHandlerFails_whenDispatching_thenIsolates() {
        willThrow(new IllegalStateException("boom")).given(tradePriceTickMessageHandler).handle(anyString());
        willThrow(new IllegalStateException("boom")).given(stockSummaryTickMessageHandler).handle(anyString());

        assertThatNoException().isThrownBy(() -> sut.dispatch("0|H0STCNT0|1|x"));

        then(stockSummaryTickMessageHandler).should().handle("0|H0STCNT0|1|x");
    }

    @DisplayName("메시지가 null이어도 예외를 던지지 않고 어떤 핸들러도 호출하지 않는다")
    @Test
    void givenNullMessage_whenDispatching_thenSwallows() {
        assertThatNoException().isThrownBy(() -> sut.dispatch(null));

        then(stateTickMessageHandler).shouldHaveNoInteractions();
        then(tradePriceTickMessageHandler).shouldHaveNoInteractions();
        then(bidAskPriceTickMessageHandler).shouldHaveNoInteractions();
        then(stockSummaryTickMessageHandler).shouldHaveNoInteractions();
    }
}
