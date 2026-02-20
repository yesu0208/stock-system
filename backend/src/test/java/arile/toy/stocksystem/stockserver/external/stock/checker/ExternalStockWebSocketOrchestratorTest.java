package arile.toy.stocksystem.stockserver.external.stock.checker;

import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.listener.ExternalStockWebSocketClient;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalStockWebSocketOrchestratorTest {

    @Mock
    private ExternalStockWebSocketClient externalStockWebSocketClient;

    @Mock
    private ApprovalKeyService approvalKeyService;

    @Mock
    private ExternalStockProperties stockProperties;

    @Mock
    private MarketTimeChecker marketTimeChecker;

    @Mock
    private MarketPhaseService marketPhaseService;

    @InjectMocks
    private ExternalStockWebSocketOrchestrator orchestrator;

    @Test
    @DisplayName("장 시작 시 ApplicationReady 이벤트에서 WebSocket에 연결하고 종목 구독을 수행한다")
    void givenMarketOpen_whenOnApplicationReady_thenConnectAndSubscribe() {
        // given
        when(marketTimeChecker.isMarketOpenNow()).thenReturn(true);
        when(approvalKeyService.issueApprovalKey()).thenReturn("APPROVAL123");
        when(externalStockWebSocketClient.isConnected()).thenReturn(false);
        when(stockProperties.getOpen()).thenReturn(List.of("005930", "000660"));

        // when
        orchestrator.onApplicationReady();

        // then
        verify(externalStockWebSocketClient).connect("APPROVAL123");
        verify(externalStockWebSocketClient).subscribe("005930");
        verify(externalStockWebSocketClient).subscribe("000660");
        verify(marketPhaseService).setScheduledMarkets();
        verify(marketPhaseService, never()).closeAllMarkets();
    }

    @Test
    @DisplayName("장 마감 시 ApplicationReady 이벤트에서 WebSocket 연결을 건너뛴다")
    void givenMarketClosed_whenOnApplicationReady_thenSkipConnect() {
        // given
        when(marketTimeChecker.isMarketOpenNow()).thenReturn(false);

        // when
        orchestrator.onApplicationReady();

        // then
        verify(externalStockWebSocketClient, never()).connect(anyString());
        verify(marketPhaseService).closeAllMarkets();
    }

    @Test
    @DisplayName("장중 WebSocket이 끊긴 경우 재연결을 수행한다")
    void givenDisconnectedDuringMarketHours_whenReconnect_thenReconnect() {
        // given
        when(marketTimeChecker.isMarketOpenNow()).thenReturn(true);
        when(externalStockWebSocketClient.isConnected()).thenReturn(false);
        when(approvalKeyService.issueApprovalKey()).thenReturn("APPROVAL123");
        when(stockProperties.getOpen()).thenReturn(List.of("005930", "000660"));

        // when
        orchestrator.reconnectIfDisconnected();

        // then
        verify(externalStockWebSocketClient).connect("APPROVAL123");
        verify(stockProperties, times(1)).getOpen();
        verify(marketPhaseService).setScheduledMarkets();
    }

    @Test
    @DisplayName("장중 WebSocket이 이미 연결되어 있으면 재연결을 수행하지 않는다")
    void givenConnectedDuringMarketHours_whenReconnect_thenDoNothing() {
        // given
        when(marketTimeChecker.isMarketOpenNow()).thenReturn(true);
        when(externalStockWebSocketClient.isConnected()).thenReturn(true);

        // when
        orchestrator.reconnectIfDisconnected();

        // then
        verify(externalStockWebSocketClient, never()).connect(anyString());
    }

    @Test
    @DisplayName("장 마감 시 WebSocket을 종료하고 마켓 상태를 닫는다")
    void disconnectAtMarketClose_thenDisconnectWebSocketAndCloseMarkets() {
        // when
        orchestrator.disconnectAtMarketClose();

        // then
        verify(externalStockWebSocketClient).disconnect();
        verify(marketPhaseService).closeAllMarkets();
    }
}
