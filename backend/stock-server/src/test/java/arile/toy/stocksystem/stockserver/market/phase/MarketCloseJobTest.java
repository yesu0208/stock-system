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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

@DisplayName("[Job] 정규장 마감 정리 Job 테스트")
@ExtendWith(MockitoExtension.class)
class MarketCloseJobTest {

    private static final List<String> STOCK_CODES = List.of("005930");

    @InjectMocks private MarketCloseJob sut;

    @Mock private MarketCloseLock marketCloseLock;
    @Mock private MarketCloseCleanupService marketCloseCleanupService;
    @Mock private AccountApiClient accountApiClient;
    @Mock private MarketClosePublisher marketClosePublisher;
    @Mock private ExternalStockProperties externalStockProperties;
    @Mock private MarketCloseCoordinator marketCloseCoordinator;
    @Mock private MarketTimeChecker marketTimeChecker;

    @DisplayName("휴장일이면 락을 잡지 않고 아무것도 하지 않는다")
    @Test
    void givenHoliday_whenRunning_thenSkips() {
        given(marketTimeChecker.isTodayHoliday()).willReturn(true);

        sut.runMarketCloseJob();

        then(marketCloseLock).shouldHaveNoInteractions();
        then(marketCloseCleanupService).shouldHaveNoInteractions();
        then(accountApiClient).shouldHaveNoInteractions();
    }

    @DisplayName("다른 인스턴스가 락을 잡고 있으면 정리하지 않고, 락도 해제하지 않는다")
    @Test
    void givenLockNotAcquired_whenRunning_thenSkipsWithoutRelease() {
        given(marketCloseLock.acquire()).willReturn(false);

        sut.runMarketCloseJob();

        then(marketCloseCleanupService).shouldHaveNoInteractions();
        then(marketCloseLock).should(never()).release();
    }

    @DisplayName("마지막으로 끝난 그룹이면 정리 후 전체 정산과 정규장 마감 신호를 보낸다")
    @Test
    void givenLastGroup_whenRunning_thenCleansUpSettlesAndPublishes() {
        givenRunnable();
        given(marketCloseCoordinator.markDoneAndCheckLast()).willReturn(true);

        sut.runMarketCloseJob();

        InOrder inOrder = inOrder(marketCloseCleanupService, marketCloseCoordinator,
                accountApiClient, marketClosePublisher, marketCloseLock);
        inOrder.verify(marketCloseCleanupService).cleanUp(STOCK_CODES);
        inOrder.verify(marketCloseCoordinator).markDoneAndCheckLast();
        inOrder.verify(accountApiClient).settleAll();
        inOrder.verify(marketClosePublisher).publishMarketClose("REGULAR");
        inOrder.verify(marketCloseLock).release();
    }

    @DisplayName("아직 다른 그룹이 끝나지 않았으면 정산·마감 신호 없이 완료 표시만 한다")
    @Test
    void givenNotLastGroup_whenRunning_thenDoesNotSettle() {
        givenRunnable();
        given(marketCloseCoordinator.markDoneAndCheckLast()).willReturn(false);

        sut.runMarketCloseJob();

        then(accountApiClient).shouldHaveNoInteractions();
        then(marketClosePublisher).shouldHaveNoInteractions();
        then(marketCloseLock).should().release();
    }

    @DisplayName("정리에 실패한 건이 있어도 완료 표시와 정산까지 진행한다")
    @Test
    void givenCleanupFailures_whenRunning_thenStillSettles() {
        givenRunnable();
        given(marketCloseCleanupService.cleanUp(STOCK_CODES)).willReturn(3);
        given(marketCloseCoordinator.markDoneAndCheckLast()).willReturn(true);

        sut.runMarketCloseJob();

        then(accountApiClient).should().settleAll();
        then(marketClosePublisher).should().publishMarketClose("REGULAR");
    }

    @DisplayName("정산 중 예외가 나도 락은 반드시 해제한다")
    @Test
    void givenSettleFails_whenRunning_thenReleasesLock() {
        givenRunnable();
        given(marketCloseCoordinator.markDoneAndCheckLast()).willReturn(true);
        willThrow(new IllegalStateException("account-server down")).given(accountApiClient).settleAll();

        sut.runMarketCloseJob();

        then(marketClosePublisher).shouldHaveNoInteractions();
        then(marketCloseLock).should().release();
    }

    private void givenRunnable() {
        given(marketTimeChecker.isTodayHoliday()).willReturn(false);
        given(marketCloseLock.acquire()).willReturn(true);
        given(externalStockProperties.getOpen()).willReturn(STOCK_CODES);
    }
}
