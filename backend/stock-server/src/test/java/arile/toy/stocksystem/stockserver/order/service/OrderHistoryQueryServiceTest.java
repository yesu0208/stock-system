package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trade.dto.TradeHistoryItem;
import arile.toy.stocksystem.stockserver.trade.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 주문·체결 내역 조회 테스트")
@ExtendWith(MockitoExtension.class)
class OrderHistoryQueryServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-25T00:00:00Z");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @InjectMocks private OrderHistoryQueryService sut;

    @Mock private OrderRepository orderRepository;
    @Mock private TradeRepository tradeRepository;

    @DisplayName("주문 내역은 상태 조건 없이(null) 조회하고, 엔티티를 내역 항목으로 변환한다")
    @Test
    void givenConditions_whenGettingOrderHistory_thenSearchesWithoutStatusFilter() {
        // Given
        OrderEntity entity = orderEntity(1L, OrderStatus.FILLED);
        given(orderRepository.search(eq(USERNAME), eq(STOCK_CODE), isNull(), eq(FROM), eq(TO), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(List.of(entity), PAGEABLE, 1));

        // When
        HistoryPageResponse<OrderHistoryItem> result =
                sut.getOrderHistory(USERNAME, STOCK_CODE, FROM, TO, PAGEABLE);

        // Then
        assertThat(result.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.orderId()).isEqualTo(1L);
                    assertThat(item.orderStatus()).isEqualTo(OrderStatus.FILLED);
                });
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("취소 내역은 CANCELED 상태로만 조회한다")
    @Test
    void givenConditions_whenGettingCancelHistory_thenSearchesCanceledOnly() {
        // Given
        given(orderRepository.search(USERNAME, STOCK_CODE, List.of(OrderStatus.CANCELED), FROM, TO, PAGEABLE))
                .willReturn(Page.empty(PAGEABLE));

        // When
        HistoryPageResponse<OrderHistoryItem> result =
                sut.getCancelHistory(USERNAME, STOCK_CODE, FROM, TO, PAGEABLE);

        // Then
        assertThat(result.items()).isEmpty();
    }

    @DisplayName("미체결 주문은 열린 상태(OPEN, PARTIAL)로만 조회한다")
    @Test
    void givenConditions_whenGettingUnfilledOrders_thenSearchesOpenStatuses() {
        // Given
        given(orderRepository.search(USERNAME, null, List.of(OrderStatus.OPEN, OrderStatus.PARTIAL), null, null, PAGEABLE))
                .willReturn(Page.empty(PAGEABLE));

        // When
        HistoryPageResponse<OrderHistoryItem> result =
                sut.getUnfilledOrders(USERNAME, null, null, null, PAGEABLE);

        // Then
        assertThat(result.items()).isEmpty();
    }

    @DisplayName("다음 페이지가 있으면 hasNext가 true다")
    @Test
    void givenMorePages_whenGettingOrderHistory_thenHasNextIsTrue() {
        // Given
        Pageable firstPageOfOne = PageRequest.of(0, 1);
        given(orderRepository.search(USERNAME, null, null, null, null, firstPageOfOne))
                .willReturn(new PageImpl<>(List.of(orderEntity(1L, OrderStatus.OPEN)), firstPageOfOne, 3));

        // When
        HistoryPageResponse<OrderHistoryItem> result =
                sut.getOrderHistory(USERNAME, null, null, null, firstPageOfOne);

        // Then
        assertThat(result.totalElements()).isEqualTo(3L);
        assertThat(result.hasNext()).isTrue();
    }

    @DisplayName("체결 내역은 체결 저장소에 조건을 그대로 넘겨 조회한다")
    @Test
    void givenConditions_whenGettingTradeHistory_thenDelegatesToTradeRepository() {
        // Given
        given(tradeRepository.search(USERNAME, STOCK_CODE, FROM, TO, PAGEABLE))
                .willReturn(Page.empty(PAGEABLE));

        // When
        HistoryPageResponse<TradeHistoryItem> result =
                sut.getTradeHistory(USERNAME, STOCK_CODE, FROM, TO, PAGEABLE);

        // Then
        assertThat(result.items()).isEmpty();
        then(orderRepository).shouldHaveNoInteractions();
    }

    private OrderEntity orderEntity(Long orderId, OrderStatus status) {
        OrderEntity entity = OrderEntity.of(
                USERNAME, STOCK_CODE, OrderType.BUY, LeverageRatio.SPOT,
                70_000, 10, status, 10,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null, 105L, null);
        entity.setOrderId(orderId);
        return entity;
    }
}
