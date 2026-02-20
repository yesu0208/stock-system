package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

    @Spy
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문이 존재할 때 findByIdForUpdate 호출 시 해당 주문을 반환한다")
    void givenSavedOrder_whenFindByIdForUpdate_thenReturnOrder() {
        // given
        OrderEntity order = new OrderEntity();
        order.setOrderId(1L);
        order.setOrderStatus(OrderStatus.OPEN);

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        // when
        Optional<OrderEntity> found = orderRepository.findByIdForUpdate(1L);

        // then
        assertTrue(found.isPresent());
        assertEquals(order, found.get());
        verify(orderRepository).findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("주문이 없을 때 findByIdForUpdate 호출 시 빈 Optional을 반환한다")
    void givenNoOrder_whenFindByIdForUpdate_thenReturnsEmpty() {
        // given
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        // when
        Optional<OrderEntity> result = orderRepository.findByIdForUpdate(99L);

        // then
        assertFalse(result.isPresent());
        verify(orderRepository).findByIdForUpdate(99L);
    }

    @Test
    @DisplayName("다양한 상태의 주문 중 미체결 주문만 findAllUnfilled에서 반환된다")
    void givenOrdersWithVariousStatuses_whenFindAllUnfilled_thenReturnOnlyOpenOrders() {
        // given
        OrderEntity openOrder = new OrderEntity();
        openOrder.setOrderId(1L);
        openOrder.setOrderStatus(OrderStatus.OPEN);

        List<OrderEntity> openOrders = List.of(openOrder);

        when(orderRepository.findAllByOrderStatusIn(anyList())).thenReturn(openOrders);

        // when
        List<OrderEntity> unfilled = orderRepository.findAllUnfilled();

        // then
        assertEquals(1, unfilled.size());
        assertTrue(unfilled.contains(openOrder));
        verify(orderRepository).findAllByOrderStatusIn(anyList());
    }

    @Test
    @DisplayName("주어진 상태 목록에 맞는 주문만 findAllByOrderStatusIn에서 반환된다")
    void givenStatuses_whenFindAllByOrderStatusIn_thenReturnMatchingOrders() {
        // given
        OrderEntity openOrder = new OrderEntity();
        openOrder.setOrderId(1L);
        openOrder.setOrderStatus(OrderStatus.OPEN);

        List<OrderEntity> openOrders = List.of(openOrder);
        when(orderRepository.findAllByOrderStatusIn(List.of(OrderStatus.OPEN))).thenReturn(openOrders);

        // when
        List<OrderEntity> result = orderRepository.findAllByOrderStatusIn(List.of(OrderStatus.OPEN));

        // then
        assertEquals(openOrders, result);
        verify(orderRepository).findAllByOrderStatusIn(List.of(OrderStatus.OPEN));
    }
}
