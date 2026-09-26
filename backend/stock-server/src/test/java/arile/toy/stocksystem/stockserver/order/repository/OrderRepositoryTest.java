package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;

@DisplayName("[Repository] 주문 리포지토리 기본 메서드 테스트")
class OrderRepositoryTest {

    @SuppressWarnings("unchecked")
    @DisplayName("미체결 주문 조회는 열린 상태만 골라 종목 목록과 함께 조회한다")
    @Test
    void whenFindingAllUnfilled_thenQueriesOnlyOpenStatuses() {
        // default 메서드만 실제로 실행하고, 쿼리 메서드는 스텁
        OrderRepository sut = mock(OrderRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        List<OrderEntity> found = List.of(mock(OrderEntity.class));
        doReturn(found).when(sut).findAllByOrderStatusInAndStockCodeIn(anyList(), anyList());

        List<OrderEntity> result = sut.findAllUnfilled(List.of("005930", "000660"));

        ArgumentCaptor<List<OrderStatus>> statuses = ArgumentCaptor.forClass(List.class);
        then(sut).should().findAllByOrderStatusInAndStockCodeIn(statuses.capture(), eq(List.of("005930", "000660")));

        List<OrderStatus> expected = Arrays.stream(OrderStatus.values()).filter(OrderStatus::isOpen).toList();
        assertThat(statuses.getValue())
                .isNotEmpty()
                .containsExactlyElementsOf(expected)
                .allMatch(OrderStatus::isOpen);
        assertThat(result).isSameAs(found);
    }
}
