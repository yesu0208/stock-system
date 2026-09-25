package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.publisher.OrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("[Service] 주문 상태 변경(취소)·미체결 조회 테스트")
@ExtendWith(MockitoExtension.class)
class OrderServiceStatusTest {

    @InjectMocks private OrderService sut;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private OrderResponseEventPublisher orderResponseEventPublisher;
    @Mock private StockServerOrderResponseRepository stockServerOrderResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Mock private QueuePositionBroadcastService queuePositionBroadcastService;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @DisplayName("강제 취소: 진행 중(OPEN·PARTIAL) 주문은 CANCELED로 바꾸고 이전 상태를 돌려준다")
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OrderStatus.class, names = {"OPEN", "PARTIAL"})
    void givenOpen_whenCancelEvent_thenCanceled(OrderStatus status) {
        OrderEntity entity = givenOrder(status);

        UpdateOrderStatusResult result = sut.updateOrderStatusByCancelEvent(1L);

        assertThat(result.previousStatus()).isEqualTo(status);
        assertThat(result.orderEntity()).isSameAs(entity);
        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @DisplayName("강제 취소: 이미 종료된(FILLED·CANCELED) 주문은 상태를 바꾸지 않는다")
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OrderStatus.class, names = {"FILLED", "CANCELED"})
    void givenClosed_whenCancelEvent_thenUnchanged(OrderStatus status) {
        OrderEntity entity = givenOrder(status);

        assertThat(sut.updateOrderStatusByCancelEvent(1L).previousStatus()).isEqualTo(status);
        assertThat(entity.getOrderStatus()).isEqualTo(status);
    }

    @DisplayName("사용자 취소: 소유자·종목이 다르거나 요청자가 없으면 empty를 돌려주고 상태를 바꾸지 않는다")
    @Test
    void givenNotOwner_whenUserCancel_thenEmpty() {
        OrderEntity entity = givenOrder(OrderStatus.OPEN);

        assertThat(sut.updateOrderStatusByUserCancel(1L, "other", "005930")).isEmpty();
        assertThat(sut.updateOrderStatusByUserCancel(1L, "user", "000660")).isEmpty();
        assertThat(sut.updateOrderStatusByUserCancel(1L, null, "005930")).isEmpty();
        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @DisplayName("사용자 취소: 소유자·종목이 일치하면 CANCELED로 바꾸고, 이미 종료된 주문은 이전 상태만 돌려준다")
    @Test
    void givenOwner_whenUserCancel_thenCanceledOrUnchanged() {
        OrderEntity entity = givenOrder(OrderStatus.PARTIAL);

        Optional<UpdateOrderStatusResult> first = sut.updateOrderStatusByUserCancel(1L, "user", "005930");
        Optional<UpdateOrderStatusResult> second = sut.updateOrderStatusByUserCancel(1L, "user", "005930");

        assertThat(first).map(UpdateOrderStatusResult::previousStatus).contains(OrderStatus.PARTIAL);
        assertThat(second).map(UpdateOrderStatusResult::previousStatus).contains(OrderStatus.CANCELED);
        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @DisplayName("주문이 없으면 예외를 던진다")
    @Test
    void givenNotFound_whenCanceling_thenThrows() {
        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateOrderStatusByCancelEvent(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("order not found");
        assertThatThrownBy(() -> sut.updateOrderStatusByUserCancel(1L, "user", "005930"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("order not found");
    }

    @DisplayName("미체결 주문 조회를 저장소에 위임한다")
    @Test
    void whenFindingUnfilled_thenDelegates() {
        List<OrderEntity> orders = List.of(new OrderEntity());
        given(orderRepository.findAllUnfilled(List.of("005930"))).willReturn(orders);

        assertThat(sut.findAllUnfilledOrders(List.of("005930"))).isSameAs(orders);
    }

    private OrderEntity givenOrder(OrderStatus status) {
        OrderEntity entity = OrderEntity.of("user", "005930", OrderType.BUY, LeverageRatio.SPOT, 70_000, 10,
                status, 10, OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null, 105L, null);
        entity.setOrderId(1L);
        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }
}
