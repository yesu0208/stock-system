package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.checker.MarketTimeChecker;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.*;

@DisplayName("[Job] 애프터마켓 마감 정리 Job 테스트")
@ExtendWith(MockitoExtension.class)
class AfterMarketCloseJobTest {

    private static final List<String> STOCK_CODES = List.of("005930");

    @InjectMocks private AfterMarketCloseJob sut;

    @Mock private AfterMarketCloseLock afterMarketCloseLock;
    @Mock private MarketCloseCleanupService marketCloseCleanupService;
    @Mock private AccountApiClient accountApiClient;
    @Mock private MarketClosePublisher marketClosePublisher;
    @Mock private ExternalStockProperties externalStockProperties;
    @Mock private AfterMarketCloseCoordinator afterMarketCloseCoordinator;
    @Mock private MarketTimeChecker marketTimeChecker;

    @DisplayName("휴장일이면 아무것도 하지 않는다")
    @Test
    void givenHoliday_whenRunning_thenSkips() {
        given(marketTimeChecker.isTodayHoliday()).willReturn(true);

        sut.runAfterMarketCloseJob();

        then(afterMarketCloseLock).shouldHaveNoInteractions();
        then(marketCloseCleanupService).shouldHaveNoInteractions();
    }

    @DisplayName("다른 인스턴스가 락을 잡고 있으면 정리하지 않는다")
    @Test
    void givenLockNotAcquired_whenRunning_thenSkips() {
        given(marketTimeChecker.isTodayHoliday()).willReturn(false);
        given(afterMarketCloseLock.acquire()).willReturn(false);

        sut.runAfterMarketCloseJob();

        then(marketCloseCleanupService).shouldHaveNoInteractions();
        then(afterMarketCloseLock).should(never()).release();
    }

    @DisplayName("마지막으로 끝난 그룹이면 정리 후 전체 정산과 애프터마켓 마감 신호(AFTER)를 보낸다")
    @Test
    void givenLastGroup_whenRunning_thenSettlesAndPublishesAfter() {
        givenRunnable();
        given(afterMarketCloseCoordinator.markDoneAndCheckLast()).willReturn(true);

        sut.runAfterMarketCloseJob();

        InOrder inOrder = inOrder(marketCloseCleanupService, afterMarketCloseCoordinator,
                accountApiClient, marketClosePublisher, afterMarketCloseLock);
        inOrder.verify(marketCloseCleanupService).cleanUp(STOCK_CODES);
        inOrder.verify(afterMarketCloseCoordinator).markDoneAndCheckLast();
        inOrder.verify(accountApiClient).settleAll();
        inOrder.verify(marketClosePublisher).publishMarketClose("AFTER");
        inOrder.verify(afterMarketCloseLock).release();
    }

    @DisplayName("아직 다른 그룹이 끝나지 않았으면 정산하지 않는다")
    @Test
    void givenNotLastGroup_whenRunning_thenDoesNotSettle() {
        givenRunnable();
        given(afterMarketCloseCoordinator.markDoneAndCheckLast()).willReturn(false);

        sut.runAfterMarketCloseJob();

        then(accountApiClient).shouldHaveNoInteractions();
        then(afterMarketCloseLock).should().release();
    }

    @DisplayName("정산 중 예외가 나도 락은 반드시 해제한다")
    @Test
    void givenSettleFails_whenRunning_thenReleasesLock() {
        givenRunnable();
        given(afterMarketCloseCoordinator.markDoneAndCheckLast()).willReturn(true);
        willThrow(new IllegalStateException("account-server down")).given(accountApiClient).settleAll();

        sut.runAfterMarketCloseJob();

        then(afterMarketCloseLock).should().release();
    }

    private void givenRunnable() {
        given(marketTimeChecker.isTodayHoliday()).willReturn(false);
        given(afterMarketCloseLock.acquire()).willReturn(true);
        given(externalStockProperties.getOpen()).willReturn(STOCK_CODES);
    }
}
