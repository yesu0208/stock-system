package arile.toy.stocksystem.stockserver.external.stock.checker;

import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.listener.ExternalStockWebSocketClient;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.market.phase.GlobalMarketPhasePublisher;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@DisplayName("[Orchestrator] 외부 시세 연결·장 단계 스케줄 테스트")
@ExtendWith(MockitoExtension.class)
class ExternalStockWebSocketOrchestratorTest {

    @InjectMocks private ExternalStockWebSocketOrchestrator sut;

    @Mock private ExternalStockWebSocketClient externalStockWebSocketClient;
    @Mock private ApprovalKeyService approvalKeyService;
    @Mock private ExternalStockProperties stockProperties;
    @Mock private MarketTimeChecker marketTimeChecker;
    @Mock private MarketPhaseService marketPhaseService;
    @Mock private GlobalMarketPhasePublisher globalMarketPhasePublisher;

    @Nested
    @DisplayName("서버 기동")
    class Startup {

        @DisplayName("장중이면 장 단계 설정 → 승인키 발급·연결·종목 구독 → 현재 단계 발행")
        @Test
        void givenMarketOpen_whenReady_thenConnectsAndSubscribes() {
            given(marketTimeChecker.isMarketOpenNow()).willReturn(true);
            given(marketTimeChecker.resolvePhase()).willReturn(StockServerMarketPhase.OPEN);
            given(approvalKeyService.issueApprovalKey()).willReturn("key");
            given(stockProperties.getOpen()).willReturn(List.of("005930", "000660"));

            sut.onApplicationReady();

            InOrder inOrder = inOrder(marketPhaseService, externalStockWebSocketClient, globalMarketPhasePublisher);
            inOrder.verify(marketPhaseService).setScheduledMarkets();
            inOrder.verify(externalStockWebSocketClient).connect("key");
            inOrder.verify(externalStockWebSocketClient).subscribe("005930");
            inOrder.verify(externalStockWebSocketClient).subscribe("000660");
            inOrder.verify(globalMarketPhasePublisher).publish(StockServerMarketPhase.OPEN);
        }

        @DisplayName("장중 연결이 실패해도 예외 없이 단계를 발행한다 (서버 기동 실패 방지, 연결은 재시도 스케줄러가 담당)")
        @Test
        void givenConnectFails_whenReady_thenStillPublishes() {
            given(marketTimeChecker.isMarketOpenNow()).willReturn(true);
            given(marketTimeChecker.resolvePhase()).willReturn(StockServerMarketPhase.OPEN);
            given(approvalKeyService.issueApprovalKey()).willThrow(new IllegalStateException("api down"));

            assertThatNoException().isThrownBy(() -> sut.onApplicationReady());

            then(globalMarketPhasePublisher).should().publish(StockServerMarketPhase.OPEN);
        }

        @DisplayName("장이 닫혀 있으면 연결하지 않고 전 종목을 닫고 CLOSED를 발행한다")
        @Test
        void givenMarketClosed_whenReady_thenClosesAll() {
            given(marketTimeChecker.isMarketOpenNow()).willReturn(false);

            sut.onApplicationReady();

            then(externalStockWebSocketClient).shouldHaveNoInteractions();
            then(marketPhaseService).should().closeAllMarkets();
            then(globalMarketPhasePublisher).should().publish(StockServerMarketPhase.CLOSED);
        }
    }

    @Nested
    @DisplayName("스케줄")
    class Schedules {

        @DisplayName("휴장일이면 모든 스케줄 작업을 건너뛴다")
        @Test
        void givenHoliday_whenScheduled_thenSkipsAll() {
            given(marketTimeChecker.isTodayHoliday()).willReturn(true);

            sut.preConnectBeforeMorningCall();
            sut.connectAtMorningCall();
            sut.openRegularMarket();
            sut.openClosingCall();
            sut.publishClosedAtSessionEnd();
            sut.closeRegularMarket();
            sut.openAfterMarket();
            sut.disconnectAtAfterMarketClose();

            then(externalStockWebSocketClient).shouldHaveNoInteractions();
            then(marketPhaseService).shouldHaveNoInteractions();
            then(globalMarketPhasePublisher).shouldHaveNoInteractions();
        }

        @DisplayName("동시호가 전 사전 연결: 끊겨 있으면 연결·구독하고 장 단계는 바꾸지 않는다")
        @Test
        void givenDisconnected_whenPreConnecting_thenConnectsWithoutPhaseChange() {
            given(marketTimeChecker.isTodayHoliday()).willReturn(false);
            given(externalStockWebSocketClient.isConnected()).willReturn(false);
            given(approvalKeyService.issueApprovalKey()).willReturn("key");
            given(stockProperties.getOpen()).willReturn(List.of("005930"));

            sut.preConnectBeforeMorningCall();

            then(externalStockWebSocketClient).should().connect("key");
            then(externalStockWebSocketClient).should().subscribe("005930");
            then(marketPhaseService).shouldHaveNoInteractions();
            then(globalMarketPhasePublisher).shouldHaveNoInteractions();
        }

        @DisplayName("동시호가 전 사전 연결: 이미 연결되어 있으면 다시 연결하지 않는다")
        @Test
        void givenConnected_whenPreConnecting_thenSkipsConnect() {
            given(marketTimeChecker.isTodayHoliday()).willReturn(false);
            given(externalStockWebSocketClient.isConnected()).willReturn(true);

            sut.preConnectBeforeMorningCall();

            then(approvalKeyService).shouldHaveNoInteractions();
            then(externalStockWebSocketClient).should(never()).connect(anyString());
        }

        @DisplayName("아침 동시호가·애프터 시작: 연결이 실패해도 장 단계는 연다")
        @Test
        void givenConnectFails_whenPhaseStarts_thenStillOpensPhase() {
            given(marketTimeChecker.isTodayHoliday()).willReturn(false);
            given(approvalKeyService.issueApprovalKey()).willThrow(new IllegalStateException("api down"));

            sut.connectAtMorningCall();
            sut.openAfterMarket();

            then(marketPhaseService).should().openMorningCall();
            then(globalMarketPhasePublisher).should().publish(StockServerMarketPhase.MORNING_CALL);
            then(marketPhaseService).should().openAfterMarket();
            then(globalMarketPhasePublisher).should().publish(StockServerMarketPhase.AFTER);
        }

        @DisplayName("이미 연결되어 있으면 다시 연결하지 않는다")
        @Test
        void givenConnected_whenMorningCall_thenSkipsConnect() {
            given(marketTimeChecker.isTodayHoliday()).willReturn(false);
            given(externalStockWebSocketClient.isConnected()).willReturn(true);

            sut.connectAtMorningCall();

            then(approvalKeyService).shouldHaveNoInteractions();
            then(externalStockWebSocketClient).should(never()).connect(anyString());
        }

        @DisplayName("정규장·마감 동시호가·장 종료·애프터 종료 단계 전환을 수행한다")
        @Test
        void whenPhaseSchedulesRun_thenTransitions() {
            given(marketTimeChecker.isTodayHoliday()).willReturn(false);

            sut.openRegularMarket();
            sut.openClosingCall();
            sut.publishClosedAtSessionEnd();
            sut.closeRegularMarket();
            sut.disconnectAtAfterMarketClose();

            then(marketPhaseService).should().openRegularMarket();
            then(globalMarketPhasePublisher).should().publish(StockServerMarketPhase.OPEN);
            then(marketPhaseService).should().openClosingCall();
            then(globalMarketPhasePublisher).should().publish(StockServerMarketPhase.CLOSING_CALL);
            then(marketPhaseService).should().closeScheduledOpenMarkets();
            then(externalStockWebSocketClient).should().disconnect();
            then(marketPhaseService).should().closeAllMarkets();
            then(globalMarketPhasePublisher).should(times(2)).publish(StockServerMarketPhase.CLOSED);
        }
    }

    @Nested
    @DisplayName("재연결")
    class Reconnect {

        @DisplayName("연결을 유지할 시간이 아니거나 이미 연결되어 있으면 아무것도 하지 않는다")
        @Test
        void givenNotNeeded_whenChecking_thenNothing() {
            given(marketTimeChecker.shouldMaintainConnection()).willReturn(false, true);
            given(externalStockWebSocketClient.isConnected()).willReturn(true);

            sut.reconnectIfDisconnected();
            sut.reconnectIfDisconnected();

            then(approvalKeyService).shouldHaveNoInteractions();
            then(marketPhaseService).shouldHaveNoInteractions();
        }

        @DisplayName("끊겨 있으면 다시 연결·구독하고 장 단계를 재설정한다")
        @Test
        void givenDisconnected_whenChecking_thenReconnects() {
            given(marketTimeChecker.shouldMaintainConnection()).willReturn(true);
            given(externalStockWebSocketClient.isConnected()).willReturn(false);
            given(approvalKeyService.issueApprovalKey()).willReturn("key");
            given(stockProperties.getOpen()).willReturn(List.of("005930"));

            sut.reconnectIfDisconnected();

            then(externalStockWebSocketClient).should().connect("key");
            then(externalStockWebSocketClient).should().subscribe("005930");
            then(marketPhaseService).should().setScheduledMarkets();
        }
    }
}
