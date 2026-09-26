package arile.toy.stocksystem.stockserver.alert.repository;

import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
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

@DisplayName("[Repository] 알림 리포지토리 기본 메서드 테스트")
class AlertRepositoryTest {

    @SuppressWarnings("unchecked")
    @DisplayName("활성 알림 조회는 열린 상태만 골라 종목 목록과 함께 조회한다")
    @Test
    void whenFindingAllActive_thenQueriesOnlyOpenStatuses() {
        // default 메서드만 실제로 실행하고, 쿼리 메서드는 스텁
        AlertRepository sut = mock(AlertRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        List<AlertEntity> found = List.of(mock(AlertEntity.class));
        doReturn(found).when(sut).findAllByStatusInAndStockCodeIn(anyList(), anyList());

        List<AlertEntity> result = sut.findAllActive(List.of("005930", "000660"));

        ArgumentCaptor<List<AlertStatus>> statuses = ArgumentCaptor.forClass(List.class);
        then(sut).should().findAllByStatusInAndStockCodeIn(statuses.capture(), eq(List.of("005930", "000660")));

        List<AlertStatus> expected = Arrays.stream(AlertStatus.values()).filter(AlertStatus::isOpen).toList();
        assertThat(statuses.getValue())
                .isNotEmpty()
                .containsExactlyElementsOf(expected)
                .allMatch(AlertStatus::isOpen);
        assertThat(result).isSameAs(found);
    }
}
