package arile.toy.stocksystem.stockserver.otoco.repository;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
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

@DisplayName("[Repository] OTOCO 리포지토리 기본 메서드 테스트")
class OtocoRepositoryTest {

    @SuppressWarnings("unchecked")
    @DisplayName("미완료 OTOCO 조회는 열린 상태만 골라 종목 목록과 함께 조회한다")
    @Test
    void whenFindingAllUnfinished_thenQueriesOnlyOpenStatuses() {
        // default 메서드만 실제로 실행하고, 쿼리 메서드는 스텁
        OtocoRepository sut = mock(OtocoRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        List<OtocoEntity> found = List.of(mock(OtocoEntity.class));
        doReturn(found).when(sut).findAllByOtocoStatusInAndStockCodeIn(anyList(), anyList());

        List<OtocoEntity> result = sut.findAllUnfinished(List.of("005930", "000660"));

        ArgumentCaptor<List<OtocoStatus>> statuses = ArgumentCaptor.forClass(List.class);
        then(sut).should().findAllByOtocoStatusInAndStockCodeIn(statuses.capture(), eq(List.of("005930", "000660")));

        List<OtocoStatus> expected = Arrays.stream(OtocoStatus.values()).filter(OtocoStatus::isOpen).toList();
        assertThat(statuses.getValue())
                .isNotEmpty()
                .containsExactlyElementsOf(expected)
                .allMatch(OtocoStatus::isOpen);
        assertThat(result).isSameAs(found);
    }
}
