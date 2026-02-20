package arile.toy.stocksystem.stockserver.cancel.service;

import arile.toy.stocksystem.stockserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.stockserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.cancel.event.publisher.CancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.cancel.repository.CancelRepository;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.dto.UpdateOrderStatusResult;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CancelServiceTest {

    @Mock private OrderService orderService;
    @Mock private CancelRepository cancelRepository;
    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private CancelResponseEventPublisher cancelResponseEventPublisher;
    @Mock private StockServerOrderResponseRepository stockServerOrderResponseRepository;
    @Mock private AccountBalanceCommand accountBalanceCommand;
    @Mock private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private CancelService cancelService;

    @Test
    @DisplayName("이미 취소된 주문을 취소 요청하면 ALREADY_CANCELLED 이벤트를 발행한다")
    void givenAlreadyCancelled_whenRegisterCancel_thenPublishAlreadyCancelled() {

        OrderEntity order = mock(OrderEntity.class);

        when(orderService.updateOrderStatusByCancelEvent(1L))
                .thenReturn(new UpdateOrderStatusResult(order, OrderStatus.CANCELED));

        CancelRequestEvent request = new CancelRequestEvent(1L, "005930");

        cancelService.registerCancel(request);

        verify(cancelResponseEventPublisher)
                .publish(argThat(event ->
                        !event.success() &&
                                event.errorCode() == CancelErrorCode.ALREADY_CANCELLED));
    }

    @Test
    @DisplayName("이미 체결된 주문을 취소 요청하면 ALREADY_FILLED 이벤트를 발행한다")
    void givenAlreadyFilled_whenRegisterCancel_thenPublishAlreadyFilled() {

        OrderEntity order = mock(OrderEntity.class);

        when(orderService.updateOrderStatusByCancelEvent(1L))
                .thenReturn(new UpdateOrderStatusResult(order, OrderStatus.FILLED));

        CancelRequestEvent request = new CancelRequestEvent(1L, "005930");

        cancelService.registerCancel(request);

        verify(cancelResponseEventPublisher)
                .publish(argThat(event ->
                        !event.success() &&
                                event.errorCode() == CancelErrorCode.ALREADY_FILLED));
    }

    @Test
    @DisplayName("Open BUY 주문을 취소 요청하면 현금 환불 후 성공 이벤트를 발행한다")
    void givenOpenBuyOrder_whenRegisterCancel_thenRefundCashAndPublishSuccess() {

        OrderEntity order = mockBuyOrder();

        when(orderService.updateOrderStatusByCancelEvent(1L))
                .thenReturn(new UpdateOrderStatusResult(order, OrderStatus.OPEN));

        when(accountBalanceCommand.refundReservedCash(anyString(), anyLong()))
                .thenReturn(true);

        CancelRequestEvent request = new CancelRequestEvent(1L, "005930");

        cancelService.registerCancel(request);

        verify(cancelRepository).save(any());
        verify(orderQueueRegistry).orderCancel(1L, "005930");
        verify(stockServerOrderResponseRepository).delete("user1", 1L);
        verify(cancelResponseEventPublisher)
                .publish(argThat(CancelResponseEvent::success));
        verify(accountUpdateEventPublisher).publish("user1");
    }

    @Test
    @DisplayName("환불 실패 시 취소 요청하면 INTERNAL_ERROR 이벤트를 발행하고 예외를 던진다")
    void givenRefundFails_whenRegisterCancel_thenThrowException() {

        OrderEntity order = mockBuyOrder();

        when(orderService.updateOrderStatusByCancelEvent(1L))
                .thenReturn(new UpdateOrderStatusResult(order, OrderStatus.OPEN));

        when(accountBalanceCommand.refundReservedCash(anyString(), anyLong()))
                .thenReturn(false);

        CancelRequestEvent request = new CancelRequestEvent(1L, "005930");

        assertThrows(IllegalStateException.class,
                () -> cancelService.registerCancel(request));

        verify(cancelResponseEventPublisher)
                .publish(argThat(event ->
                        !event.success() &&
                                event.errorCode() == CancelErrorCode.INTERNAL_ERROR));
    }

    @Test
    @DisplayName("Open 상태가 아닌 주문을 강제 취소하면 아무 작업도 수행하지 않는다")
    void givenNotOpenOrder_whenForceCancel_thenDoNothing() {

        when(orderService.updateOrderStatusByCancelEvent(1L))
                .thenReturn(new UpdateOrderStatusResult(mock(OrderEntity.class),
                        OrderStatus.CANCELED));

        cancelService.forceCancel(1L);

        verifyNoInteractions(cancelRepository);
        verifyNoInteractions(accountUpdateEventPublisher);
    }

    @Test
    @DisplayName("Open SELL 주문을 강제 취소하면 주식 환불 후 취소 이벤트 발행")
    void givenOpenSellOrder_whenForceCancel_thenRefundStock() {

        // given
        OrderEntity order = mockSellOrder();

        UpdateOrderStatusResult result = mock(UpdateOrderStatusResult.class);

        when(orderService.updateOrderStatusByCancelEvent(1L))
                .thenReturn(result);

        when(result.previousStatus()).thenReturn(OrderStatus.OPEN);
        when(result.orderEntity()).thenReturn(order);

        when(accountBalanceCommand.refundReservedStock(
                anyString(), anyString(), anyInt()
        )).thenReturn(true);

        // when
        cancelService.forceCancel(1L);

        // then
        verify(cancelRepository).save(any());
        verify(stockServerOrderResponseRepository).delete("user1", 1L);
        verify(accountUpdateEventPublisher).publish("user1");
    }

    private OrderEntity mockBuyOrder() {
        OrderEntity order = mock(OrderEntity.class);

        when(order.getOrderId()).thenReturn(1L);
        when(order.getStockCode()).thenReturn("005930");
        when(order.getUsername()).thenReturn("user1");
        when(order.getOrderType()).thenReturn(OrderType.BUY);
        when(order.getOrderPrice()).thenReturn(10000);
        when(order.getRemainingQuantity()).thenReturn(10);

        return order;
    }

    private OrderEntity mockSellOrder() {
        OrderEntity order = mock(OrderEntity.class);

        when(order.getOrderId()).thenReturn(1L);
        when(order.getStockCode()).thenReturn("005930");
        when(order.getUsername()).thenReturn("user1");
        when(order.getOrderType()).thenReturn(OrderType.SELL);
        when(order.getRemainingQuantity()).thenReturn(10);

        return order;
    }
}
