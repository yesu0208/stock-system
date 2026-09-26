package arile.toy.stocksystem.stockserver.autoorder.repository;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
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

@DisplayName("[Repository] 자동 주문 리포지토리 기본 메서드 테스트")
class AutoOrderRepositoryTest {

    @SuppressWarnings("unchecked")
    @DisplayName("미발동 자동 주문 조회는 열린 상태만 골라 종목 목록과 함께 조회한다")
    @Test
    void whenFindingAllUntriggered_thenQueriesOnlyOpenStatuses() {
        // default 메서드만 실제로 실행하고, 쿼리 메서드는 스텁
        AutoOrderRepository sut = mock(AutoOrderRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        List<AutoOrderEntity> found = List.of(mock(AutoOrderEntity.class));
        doReturn(found).when(sut).findAllByAutoOrderStatusInAndStockCodeIn(anyList(), anyList());

        List<AutoOrderEntity> result = sut.findAllUntriggered(List.of("005930", "000660"));

        ArgumentCaptor<List<AutoOrderStatus>> statuses = ArgumentCaptor.forClass(List.class);
        then(sut).should().findAllByAutoOrderStatusInAndStockCodeIn(statuses.capture(), eq(List.of("005930", "000660")));

        List<AutoOrderStatus> expected = Arrays.stream(AutoOrderStatus.values()).filter(AutoOrderStatus::isOpen).toList();
        assertThat(statuses.getValue())
                .isNotEmpty()
                .containsExactlyElementsOf(expected)
                .allMatch(AutoOrderStatus::isOpen);
        assertThat(result).isSameAs(found);
    }
}
