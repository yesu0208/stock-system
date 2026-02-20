package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderService;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.useraccount.service.UserAccountService;
import arile.toy.stocksystem.stockserver.userstock.service.UserStockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

class MarketCloseJobTest {

    @Mock private MarketCloseLock marketCloseLock;
    @Mock private OrderService orderService;
    @Mock private CancelService cancelService;
    @Mock private AutoOrderService autoOrderService;
    @Mock private AutoCancelService autoCancelService;
    @Mock private UserAccountService userAccountService;
    @Mock private UserStockService userStockService;
    @Mock private MarketClosePublisher marketClosePublisher;

    @InjectMocks
    private MarketCloseJob marketCloseJob;

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("락을 획득하지 못하면 Market Close Job 실행을 건너뛴다")
    void givenLockNotAcquired_whenRunMarketCloseJob_thenSkip() {
        // Given
        when(marketCloseLock.acquire()).thenReturn(false);

        // When
        marketCloseJob.runMarketCloseJob();

        // Then
        verify(marketCloseLock, never()).release();
        verify(orderService, never()).findAllUnfilledOrders();
        verify(autoOrderService, never()).findAllUntriggeredAutoOrders();
    }

    @Test
    @DisplayName("락을 획득하면 모든 Market Close 처리 수행")
    void givenLockAcquired_whenRunMarketCloseJob_thenProcessesAll() {
        // Given
        when(marketCloseLock.acquire()).thenReturn(true);

        OrderEntity order1 = mock(OrderEntity.class);
        when(order1.getOrderId()).thenReturn(1L);
        when(order1.getUsername()).thenReturn("user1");

        AutoOrderEntity autoOrder1 = mock(AutoOrderEntity.class);
        when(autoOrder1.getAutoOrderId()).thenReturn(2L);
        when(autoOrder1.getUsername()).thenReturn("user2");

        when(orderService.findAllUnfilledOrders()).thenReturn(List.of(order1));
        when(autoOrderService.findAllUntriggeredAutoOrders()).thenReturn(List.of(autoOrder1));

        // When
        marketCloseJob.runMarketCloseJob();

        // Then
        verify(cancelService).forceCancel(1L);
        verify(autoCancelService).forceAutoCancel(2L);

        Set<String> usernames = Stream.of("user1", "user2").collect(Collectors.toSet());
        verify(userAccountService).settleAccounts(usernames);
        verify(userStockService).settleStocks(usernames);
        verify(marketClosePublisher).publishMarketClose();

        verify(marketCloseLock).release();
    }

    @Test
    @DisplayName("Market Close Job 실행 중 예외 발생 시에도 Lock은 반드시 해제된다")
    void givenException_whenRunMarketCloseJob_thenLockReleased() {
        // Given
        when(marketCloseLock.acquire()).thenReturn(true);
        when(orderService.findAllUnfilledOrders()).thenThrow(new RuntimeException("DB error"));

        // When
        marketCloseJob.runMarketCloseJob();

        // Then
        verify(marketCloseLock).release();
    }
}
