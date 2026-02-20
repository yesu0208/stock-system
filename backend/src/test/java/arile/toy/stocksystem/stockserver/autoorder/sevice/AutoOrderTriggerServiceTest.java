package arile.toy.stocksystem.stockserver.autoorder.sevice;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AutoStockLockRegistry;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class AutoOrderTriggerServiceTest {

    private AutoOrderQueueRegistry autoOrderQueueRegistry;
    private AutoStockLockRegistry autoStockLockRegistry;
    private AutoOrderService autoOrderService;
    private StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private OrderService orderService;
    private AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;

    private AutoOrderTriggerService service;

    @BeforeEach
    void setUp() {
        autoOrderQueueRegistry = mock(AutoOrderQueueRegistry.class);
        autoStockLockRegistry = mock(AutoStockLockRegistry.class);
        autoOrderService = mock(AutoOrderService.class);
        stockServerAutoOrderResponseRepository = mock(StockServerAutoOrderResponseRepository.class);
        orderService = mock(OrderService.class);
        autoOrderResponseEventPublisher = mock(AutoOrderResponseEventPublisher.class);

        service = new AutoOrderTriggerService(
                autoOrderQueueRegistry,
                autoStockLockRegistry,
                autoOrderService,
                stockServerAutoOrderResponseRepository,
                orderService,
                autoOrderResponseEventPublisher
        );
    }

    @Test
    @DisplayName("BUY 주문이 큐에 있을 때 가격이 트리거에 도달하면 주문 등록 및 이벤트 발행")
    void givenBuyOrderInQueue_whenTickPriceReachesTrigger_thenOrderIsRegisteredAndEventPublished() {
        String stockCode = "005930";
        TradePriceTickMessage message = new TradePriceTickMessage(
                TickMessageType.TRADEPRICE, "005930", Instant.now().toString(),
                50000, 500, 51000, 53000, 50000,
                5, 50000, 5000000L, 25000,
                25000, "5", 30000
        );

        ReentrantLock lock = new ReentrantLock();
        when(autoStockLockRegistry.lock(stockCode)).thenReturn(lock);

        Instant now = Instant.now();
        AutoOrderDto buyOrder = new AutoOrderDto(
                1L,
                "user1",
                stockCode,
                AutoOrderType.BUY,
                49500,
                50000,
                10,
                now
        );

        when(autoOrderQueueRegistry.pollBuy(stockCode))
                .thenReturn(buyOrder)
                .thenReturn(null);

        UpdateAutoOrderStatusResult result = mock(UpdateAutoOrderStatusResult.class);
        when(result.previousStatus()).thenReturn(AutoOrderStatus.ACTIVE);
        when(autoOrderService.updateAutoOrderStatusByTrigger(anyLong())).thenReturn(result);

        service.getExternalTickMessageAndTrigger(message);

        verify(autoOrderQueueRegistry, atLeastOnce()).pollBuy(stockCode);
        verify(autoOrderService).updateAutoOrderStatusByTrigger(1L);
        verify(orderService).registerOrder(any(), eq(true));
        verify(stockServerAutoOrderResponseRepository).delete("user1", 1L);
        verify(autoOrderResponseEventPublisher).publishTrigger("user1");
    }

    @Test
    @DisplayName("SELL 주문이 큐에 있을 때 가격이 트리거에 도달하면 주문 등록 및 이벤트 발행")
    void givenSellOrderInQueue_whenTickPriceReachesTrigger_thenOrderIsRegisteredAndEventPublished() {
        String stockCode = "005930";
        TradePriceTickMessage message = new TradePriceTickMessage(
                TickMessageType.TRADEPRICE, "005930", Instant.now().toString(),
                50000, 500, 51000, 53000, 50000,
                5, 50000, 5000000L, 25000,
                25000, "5", 30000
        );

        ReentrantLock lock = new ReentrantLock();
        when(autoStockLockRegistry.lock(stockCode)).thenReturn(lock);

        Instant now = Instant.now();
        AutoOrderDto sellOrder = new AutoOrderDto(
                2L,
                "user1",
                stockCode,
                AutoOrderType.SELL,
                50500,
                51000,
                5,
                now
        );

        when(autoOrderQueueRegistry.pollSell(stockCode))
                .thenReturn(sellOrder)
                .thenReturn(null);

        UpdateAutoOrderStatusResult result = mock(UpdateAutoOrderStatusResult.class);
        when(result.previousStatus()).thenReturn(AutoOrderStatus.ACTIVE);
        when(autoOrderService.updateAutoOrderStatusByTrigger(anyLong())).thenReturn(result);

        service.getExternalTickMessageAndTrigger(message);

        verify(autoOrderQueueRegistry, atLeastOnce()).pollSell(stockCode);
        verify(autoOrderService).updateAutoOrderStatusByTrigger(2L);
        verify(orderService).registerOrder(any(), eq(true));
        verify(stockServerAutoOrderResponseRepository).delete("user1", 2L);
        verify(autoOrderResponseEventPublisher).publishTrigger("user1");
    }

    @Test
    @DisplayName("Stock lock 사용 시 Tick 처리 후 Lock이 반드시 해제된다")
    void givenStockLock_whenProcessingTick_thenLockIsReleasedAfterProcessing() {
        String stockCode = "005930";
        TradePriceTickMessage message = new TradePriceTickMessage(
                TickMessageType.TRADEPRICE, "005930", Instant.now().toString(),
                50000, 500, 51000, 53000, 50000,
                5, 50000, 5000000L, 25000,
                25000, "5", 30000
        );

        ReentrantLock lock = spy(new ReentrantLock());
        when(autoStockLockRegistry.lock(stockCode)).thenReturn(lock);

        service.getExternalTickMessageAndTrigger(message);

        assertFalse(lock.isHeldByCurrentThread(), "Lock should be released after processing");
    }
}
