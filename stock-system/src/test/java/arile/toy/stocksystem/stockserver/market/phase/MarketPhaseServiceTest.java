package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketPhaseServiceTest {

    @Mock
    private StockServerMarketPhaseRegistry registry;

    @Mock
    private ExternalStockProperties stockProperties;

    @Mock
    private MarketPhasePublisher marketPhasePublisher;

    private MarketPhaseService marketPhaseService;

    @BeforeEach
    void setup() {
        marketPhaseService = new MarketPhaseService(registry, stockProperties, marketPhasePublisher);
    }

    @Test
    @DisplayName("Closing call 이후 거래 시간일 때 마켓을 닫으면 CLOSED 상태로 변경 및 이벤트 발행")
    void givenTradeTimeAfterClosingCall_whenCloseMarketAfterClosingCall_thenMarketClosed() {
        String stockCode = "000660";
        String tradeTime = "153000";

        when(registry.isClosed(stockCode)).thenReturn(false);

        marketPhaseService.closeMarketAfterClosingCall(stockCode, tradeTime);

        verify(registry).setPhase(stockCode, StockServerMarketPhase.CLOSED);
        verify(marketPhasePublisher).publish(stockCode, StockServerMarketPhase.CLOSED);
    }

    @Test
    @DisplayName("스케줄된 마켓 설정 시 OPEN/Closed 상태 업데이트")
    void givenScheduledMarkets_whenSetScheduledMarkets_thenOpenAndCloseMarketsUpdated() {
        when(stockProperties.getOpen()).thenReturn(List.of("000660", "005930"));
        when(stockProperties.getClose()).thenReturn(List.of("035420"));

        when(registry.isOpened(anyString())).thenReturn(false);
        when(registry.isClosed(anyString())).thenReturn(false);

        marketPhaseService.setScheduledMarkets();

        verify(registry).setPhase("000660", StockServerMarketPhase.OPEN);
        verify(registry).setPhase("005930", StockServerMarketPhase.OPEN);
        verify(registry).setPhase("035420", StockServerMarketPhase.CLOSED);
    }

    @Test
    @DisplayName("모든 마켓 닫기 시 모든 마켓이 CLOSED 상태로 변경")
    void givenAllMarkets_whenCloseAllMarkets_thenAllMarketsClosed() {
        when(stockProperties.getOpen()).thenReturn(List.of("000660", "005930"));
        when(stockProperties.getClose()).thenReturn(List.of("035420"));

        when(registry.isClosed(anyString())).thenReturn(false);

        marketPhaseService.closeAllMarkets();

        verify(registry).setPhase("000660", StockServerMarketPhase.CLOSED);
        verify(registry).setPhase("005930", StockServerMarketPhase.CLOSED);
        verify(registry).setPhase("035420", StockServerMarketPhase.CLOSED);
    }

    @Test
    @DisplayName("이미 OPEN 상태인 마켓은 updateMarketPhase 호출 시 업데이트하지 않음")
    void givenMarketAlreadyOpen_whenUpdateMarketPhase_thenNoUpdate() {
        String stockCode = "000660";

        when(registry.isOpened(stockCode)).thenReturn(true);

        marketPhaseService.updateMarketPhase(stockCode, StockServerMarketPhase.OPEN);

        verify(registry, never()).setPhase(anyString(), any());
        verify(marketPhasePublisher, never()).publish(anyString(), any());
    }

    @Test
    @DisplayName("이미 CLOSED 상태인 마켓은 updateMarketPhase 호출 시 업데이트하지 않음")
    void givenMarketAlreadyClosed_whenUpdateMarketPhase_thenNoUpdate() {
        String stockCode = "000660";

        when(registry.isClosed(stockCode)).thenReturn(true);

        marketPhaseService.updateMarketPhase(stockCode, StockServerMarketPhase.CLOSED);

        verify(registry, never()).setPhase(anyString(), any());
        verify(marketPhasePublisher, never()).publish(anyString(), any());
    }
}
