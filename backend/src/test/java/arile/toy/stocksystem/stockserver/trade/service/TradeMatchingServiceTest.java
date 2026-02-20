package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.event.publisher.TradeResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trade.TradeCommand;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeMatchingServiceTest {

    @Mock private StockLockRegistry stockLockRegistry;
    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private TradeExecutionService tradeExecutionService;
    @Mock
    private TradeResponseEventPublisher tradeResponseEventPublisher;
    @Mock private StockServerOrderResponseRepository stockServerOrderResponseRepository;
    @Mock private TradeCommand tradeCommand;
    @Mock private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private TradeMatchingService tradeMatchingService;

    /* ===============================
        매도 체결 성공
       =============================== */

    @Test
    void givenSellOrder_whenPriceMatch_thenExecuteSellTrade() {

        ReentrantLock lock = new ReentrantLock();
        when(stockLockRegistry.lock("005930")).thenReturn(lock);

        TradePriceTickMessage tick = mock(TradePriceTickMessage.class);
        when(tick.stockCode()).thenReturn("005930");
        when(tick.curPrice()).thenReturn(50000);
        when(tick.tradingVolumeTick()).thenReturn(10);
        when(tick.tradingType()).thenReturn("1");

        OrderDto sellOrder = new OrderDto(
                1L, "user1", "005930",
                OrderType.SELL,
                49000,
                10,
                10,
                OrderStatus.OPEN,
                Instant.now()
        );

        when(orderQueueRegistry.pollSell("005930"))
                .thenReturn(sellOrder)
                .thenReturn(null);

        TradeEntity tradeEntity = mock(TradeEntity.class);

        TradeResult result = TradeResult.of(
                tradeEntity,
                500000L,
                10
        );

        when(tradeExecutionService.executeSellTrade(sellOrder, 50000, 10))
                .thenReturn(result);

        when(tradeCommand.applySellTrade(any(), any(), anyInt(), anyLong(), anyLong(), anyLong()))
                .thenReturn(true);

        tradeMatchingService.getExternalTickMessageAndTrade(tick);

        verify(tradeExecutionService)
                .executeSellTrade(sellOrder, 50000, 10);

        verify(stockServerOrderResponseRepository)
                .delete("user1", 1L);

        verify(tradeResponseEventPublisher).publish(any());
        verify(accountUpdateEventPublisher).publish("user1");
    }


    /* ===============================
        가격 불일치 → 재큐잉
       =============================== */

    @Test
    void givenSellOrder_whenPriceNotMatch_thenRequeue() {

        ReentrantLock lock = new ReentrantLock();
        when(stockLockRegistry.lock("005930")).thenReturn(lock);

        TradePriceTickMessage tick = mock(TradePriceTickMessage.class);
        when(tick.stockCode()).thenReturn("005930");
        when(tick.curPrice()).thenReturn(48000);
        when(tick.tradingVolumeTick()).thenReturn(10);
        when(tick.tradingType()).thenReturn("1");

        OrderDto sellOrder = new OrderDto(
                1L, "user1", "005930",
                OrderType.SELL,
                50000,
                10,
                10,
                OrderStatus.OPEN,
                Instant.now()
        );

        when(orderQueueRegistry.pollSell("005930"))
                .thenReturn(sellOrder);

        tradeMatchingService.getExternalTickMessageAndTrade(tick);

        verify(orderQueueRegistry).orderEnqueue(sellOrder);
        verify(tradeExecutionService, never())
                .executeSellTrade(any(), anyInt(), anyInt());
    }


    /* ===============================
        부분 체결 → 재큐잉 + update
       =============================== */

    @Test
    void givenPartialExecution_whenRemainingExists_thenReenqueue() {

        ReentrantLock lock = new ReentrantLock();
        when(stockLockRegistry.lock("005930")).thenReturn(lock);

        TradePriceTickMessage tick = mock(TradePriceTickMessage.class);
        when(tick.stockCode()).thenReturn("005930");
        when(tick.curPrice()).thenReturn(50000);
        when(tick.tradingVolumeTick()).thenReturn(5);
        when(tick.tradingType()).thenReturn("1");

        OrderDto sellOrder = new OrderDto(
                1L, "user1", "005930",
                OrderType.SELL,
                49000,
                10,
                10,
                OrderStatus.OPEN,
                Instant.now()
        );

        when(orderQueueRegistry.pollSell("005930"))
                .thenReturn(sellOrder)
                .thenReturn(null);

        TradeResult result = TradeResult.of(
                mock(TradeEntity.class),
                250000L,
                5
        );

        when(tradeExecutionService.executeSellTrade(sellOrder, 50000, 5))
                .thenReturn(result);

        when(tradeCommand.applySellTrade(any(), any(), anyInt(), anyLong(), anyLong(), anyLong()))
                .thenReturn(true);

        tradeMatchingService.getExternalTickMessageAndTrade(tick);

        verify(orderQueueRegistry).orderEnqueue(argThat(o ->
                o.remainingQuantity() == 5
        ));

        verify(stockServerOrderResponseRepository)
                .update(eq("user1"), eq(1L), any());

        verify(tradeResponseEventPublisher).publish(any());
        verify(accountUpdateEventPublisher).publish("user1");
    }
}
