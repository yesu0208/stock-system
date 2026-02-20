package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.repository.TradeRepository;
import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.stockserver.useraccount.repository.UserAccountRepository;
import arile.toy.stocksystem.stockserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.stockserver.userstock.repository.UserStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeExecutionServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock private UserStockRepository userStockRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private TradeExecutionService tradeExecutionService;

    /* =========================================
        executeBuyTrade()
       ========================================= */

    @Test
    void givenOpenBuyOrder_whenExecuteBuyTrade_thenSuccess() {

        // given
        OrderDto dto = new OrderDto(1L, "user1", "005930", OrderType.BUY,
                50000, 10, 10, OrderStatus.OPEN, Instant.now());

        OrderEntity order = mock(OrderEntity.class);
        UserAccountEntity account = mock(UserAccountEntity.class);
        UserStockEntity userStock = mock(UserStockEntity.class);
        TradeEntity tradeEntity = mock(TradeEntity.class);

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        when(order.getOrderStatus()).thenReturn(OrderStatus.OPEN);

        when(userAccountRepository.findByUsernameForUpdate("user1"))
                .thenReturn(Optional.of(account));

        when(account.getBalance()).thenReturn(1_000_000L);

        when(userStockRepository.findByUsernameAndStockCode("user1", "005930"))
                .thenReturn(Optional.of(userStock));

        when(userStock.getQuantity()).thenReturn(5);
        when(userStock.getAmount()).thenReturn(500_000L);

        when(tradeRepository.save(any()))
                .thenReturn(tradeEntity);

        // when
        TradeResult result = tradeExecutionService.executeBuyTrade(dto, 50000, 5);

        // then
        assertNotNull(result);

        verify(account).setBalance(1_000_000L - 250_000L);
        verify(userStock).setQuantity(10);
        verify(userStock).setAmount(750_000L);
        verify(order).setOrderStatus(OrderStatus.PARTIAL);
        verify(order).setRemainingQuantity(5);
        verify(tradeRepository).save(any());
    }

    @Test
    void givenInsufficientBalance_whenExecuteBuyTrade_thenThrowException() {

        OrderDto dto = new OrderDto(1L, "user1", "005930", OrderType.BUY,
                50000, 50, 50, OrderStatus.OPEN, Instant.now());

        OrderEntity order = mock(OrderEntity.class);
        UserAccountEntity account = mock(UserAccountEntity.class);

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        when(order.getOrderStatus()).thenReturn(OrderStatus.OPEN);

        when(userAccountRepository.findByUsernameForUpdate("user1"))
                .thenReturn(Optional.of(account));

        when(account.getBalance()).thenReturn(1000L);

        assertThrows(IllegalStateException.class,
                () -> tradeExecutionService.executeBuyTrade(dto, 10000, 5));
    }

    /* =========================================
        executeSellTrade()
       ========================================= */

    @Test
    void givenOpenSellOrder_whenExecuteSellTrade_thenSuccess() {

        OrderDto dto = new OrderDto(1L, "user1", "005930", OrderType.SELL,
                50000, 10, 10, OrderStatus.OPEN, Instant.now());

        OrderEntity order = mock(OrderEntity.class);
        UserAccountEntity account = mock(UserAccountEntity.class);
        UserStockEntity userStock = mock(UserStockEntity.class);
        TradeEntity tradeEntity = mock(TradeEntity.class);

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        when(order.getOrderStatus()).thenReturn(OrderStatus.OPEN);

        when(userAccountRepository.findByUsernameForUpdate("user1"))
                .thenReturn(Optional.of(account));

        when(account.getBalance()).thenReturn(0L);

        when(userStockRepository.findByUsernameAndStockCode("user1", "005930"))
                .thenReturn(Optional.of(userStock));

        when(userStock.getQuantity()).thenReturn(10);
        when(userStock.getAmount()).thenReturn(1000_000L);

        when(tradeRepository.save(any()))
                .thenReturn(tradeEntity);

        TradeResult result = tradeExecutionService.executeSellTrade(dto, 50000, 5);

        assertNotNull(result);

        verify(account).setBalance(250_000L);
        verify(userStock).setQuantity(5);
        verify(userStock).setAmount(500_000L);
        verify(order).setOrderStatus(OrderStatus.PARTIAL);
        verify(order).setRemainingQuantity(5);
    }

    @Test
    void givenNoStockOwned_whenExecuteSellTrade_thenThrowException() {

        OrderDto dto = new OrderDto(1L, "user1", "005930", OrderType.BUY,
                50000, 50, 0, OrderStatus.OPEN, Instant.now());

        OrderEntity order = mock(OrderEntity.class);

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        when(order.getOrderStatus()).thenReturn(OrderStatus.OPEN);

        when(userAccountRepository.findByUsernameForUpdate("user1"))
                .thenReturn(Optional.of(mock(UserAccountEntity.class)));

        when(userStockRepository.findByUsernameAndStockCode("user1", "005930"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> tradeExecutionService.executeSellTrade(dto, 10000, 5));
    }
}
