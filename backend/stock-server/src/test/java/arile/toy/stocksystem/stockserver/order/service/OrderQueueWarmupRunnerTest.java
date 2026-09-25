package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 서버 시작 시 미체결 주문 대기열 복구 테스트")
@ExtendWith(MockitoExtension.class)
class OrderQueueWarmupRunnerTest {

    @InjectMocks private OrderQueueWarmupRunner sut;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private ExternalStockProperties externalStockProperties;

    @DisplayName("담당 종목이 없으면 조회·복구를 하지 않는다")
    @Test
    void givenNoStocks_whenWarmingUp_thenDoesNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of());

        sut.warmUp();

        then(orderRepository).shouldHaveNoInteractions();
        then(orderQueueRegistry).shouldHaveNoInteractions();
    }

    @DisplayName("담당 종목의 미체결 주문을 잔량과 함께 대기열에 다시 등록한다")
    @Test
    void givenUnfilledOrders_whenWarmingUp_thenEnqueuesEach() {
        // Given
        List<String> stockCodes = List.of("005930", "000660");
        given(externalStockProperties.getOpen()).willReturn(stockCodes);

        OrderEntity open = order(1L, "005930", OrderType.BUY, OrderStatus.OPEN, 10);
        OrderEntity partial = order(2L, "000660", OrderType.SELL, OrderStatus.PARTIAL, 4);
        given(orderRepository.findAllUnfilled(stockCodes)).willReturn(List.of(open, partial));

        // When
        sut.warmUp();

        // Then
        ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
        then(orderQueueRegistry).should(times(2)).orderEnqueue(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(OrderDto::orderId, OrderDto::stockCode, OrderDto::orderType,
                        OrderDto::orderStatus, OrderDto::remainingQuantity)
                .containsExactly(
                        tuple(1L, "005930", OrderType.BUY, OrderStatus.OPEN, 10),
                        tuple(2L, "000660", OrderType.SELL, OrderStatus.PARTIAL, 4));
    }

    @DisplayName("저장된 주문 시간을 그대로 복구해 재시작 후에도 시간 우선순위가 유지된다")
    @Test
    void givenUnfilledOrder_whenWarmingUp_thenKeepsOriginalOrderTime() {
        // Given
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        OrderEntity entity = order(1L, "005930", OrderType.BUY, OrderStatus.OPEN, 10);
        given(orderRepository.findAllUnfilled(List.of("005930"))).willReturn(List.of(entity));

        // When
        sut.warmUp();

        // Then
        ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
        then(orderQueueRegistry).should().orderEnqueue(captor.capture());
        assertThat(captor.getValue().orderTime()).isEqualTo(entity.getOrderTime());
    }

    @DisplayName("미체결 주문이 없으면 대기열에 아무것도 등록하지 않는다")
    @Test
    void givenNoUnfilledOrders_whenWarmingUp_thenEnqueuesNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        given(orderRepository.findAllUnfilled(List.of("005930"))).willReturn(List.of());

        sut.warmUp();

        then(orderQueueRegistry).should(never()).orderEnqueue(any());
    }

    private OrderEntity order(Long orderId, String stockCode, OrderType type, OrderStatus status, int remaining) {
        OrderEntity entity = OrderEntity.of(
                "user", stockCode, type, LeverageRatio.SPOT,
                70_000, 10, status, remaining,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null, null, null);
        entity.setOrderId(orderId);
        return entity;
    }
}
