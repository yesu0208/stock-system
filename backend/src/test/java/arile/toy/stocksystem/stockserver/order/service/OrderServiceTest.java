package arile.toy.stocksystem.stockserver.order.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.event.publisher.OrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderQueueRegistry orderQueueRegistry;

    @Mock
    private OrderResponseEventPublisher orderResponseEventPublisher;

    @Mock
    private StockServerOrderResponseRepository stockServerOrderResponseRepository;

    @Mock
    private AccountBalanceCommand accountBalanceCommand;

    @Mock
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 등록 성공 - BUY 타입")
    void givenBuyOrder_whenRegister_thenReserveCashAndEnqueue() {
        // given
        StockServerOrderRequestEvent request = mock(StockServerOrderRequestEvent.class);
        when(request.orderType()).thenReturn(OrderType.BUY);
        when(request.orderPrice()).thenReturn(100);
        when(request.orderQuantity()).thenReturn(2);
        when(request.username()).thenReturn("user1");
        when(request.stockCode()).thenReturn("005930");

        when(accountBalanceCommand.reserveCash("user1", 200L)).thenReturn(true);

        OrderEntity savedOrder = OrderEntity.of(
                "user1", "AAPL", OrderType.BUY, 100, 2, OrderStatus.OPEN,
                2
        );
        when(orderRepository.save(any())).thenReturn(savedOrder);

        // when
        orderService.registerOrder(request, false);

        // then
        verify(accountBalanceCommand).reserveCash("user1", 200L);
        verify(orderQueueRegistry).orderEnqueue(any(OrderDto.class));
        verify(stockServerOrderResponseRepository).save(any());
        verify(orderResponseEventPublisher).publish(any());
        verify(accountUpdateEventPublisher).publish("user1");
    }

    @Test
    @DisplayName("주문 등록 실패 - 잔액 부족")
    void givenBuyOrder_whenReserveCashFails_thenPublishError() {
        // given
        StockServerOrderRequestEvent request = mock(StockServerOrderRequestEvent.class);
        when(request.orderType()).thenReturn(OrderType.BUY);
        when(request.orderPrice()).thenReturn(100);
        when(request.orderQuantity()).thenReturn(2);
        when(request.username()).thenReturn("user1");

        when(accountBalanceCommand.reserveCash("user1", 200L)).thenReturn(false);

        // when
        orderService.registerOrder(request, false);

        // then
        verify(orderResponseEventPublisher)
                .publishError(request, OrderErrorCode.INSUFFICIENT_BALANCE);
        verifyNoInteractions(orderQueueRegistry);
        verifyNoInteractions(stockServerOrderResponseRepository);
    }

    @Test
    @DisplayName("주문 상태 취소 - OPEN 상태")
    void givenOpenOrder_whenCancel_thenStatusChanged() {
        // given
        OrderEntity entity = OrderEntity.of(
                "user1", "AAPL", OrderType.BUY, 100, 2, OrderStatus.OPEN,
                2
        );
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));

        // when
        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(1L);

        // then
        assertThat(result.previousStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 상태 취소 - 이미 CANCELED 상태")
    void givenCanceledOrder_whenCancel_thenNoChange() {
        // given
        OrderEntity entity = OrderEntity.of(
                "user1", "AAPL", OrderType.BUY, 100, 2, OrderStatus.CANCELED,
                0
        );
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));

        // when
        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(1L);

        // then
        assertThat(result.previousStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("모든 미체결 주문 조회")
    void givenOrders_whenFindAllUnfilled_thenReturnList() {
        // given
        OrderEntity order1 = mock(OrderEntity.class);
        OrderEntity order2 = mock(OrderEntity.class);
        when(orderRepository.findAllUnfilled()).thenReturn(List.of(order1, order2));

        // when
        List<OrderEntity> result = orderService.findAllUnfilledOrders();

        // then
        assertThat(result).hasSize(2);
    }
}
