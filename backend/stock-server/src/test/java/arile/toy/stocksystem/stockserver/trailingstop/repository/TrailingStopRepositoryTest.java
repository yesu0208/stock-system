package arile.toy.stocksystem.stockserver.trailingstop.repository;

import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
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

@DisplayName("[Repository] 트레일링 스탑 리포지토리 기본 메서드 테스트")
class TrailingStopRepositoryTest {

    @SuppressWarnings("unchecked")
    @DisplayName("미발동 트레일링 스탑 조회는 열린 상태만 골라 종목 목록과 함께 조회한다")
    @Test
    void whenFindingAllUntriggered_thenQueriesOnlyOpenStatuses() {
        // default 메서드만 실제로 실행하고, 쿼리 메서드는 스텁
        TrailingStopRepository sut = mock(TrailingStopRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        List<TrailingStopEntity> found = List.of(mock(TrailingStopEntity.class));
        doReturn(found).when(sut).findAllByTrailingStopStatusInAndStockCodeIn(anyList(), anyList());

        List<TrailingStopEntity> result = sut.findAllUntriggered(List.of("005930", "000660"));

        ArgumentCaptor<List<TrailingStopStatus>> statuses = ArgumentCaptor.forClass(List.class);
        then(sut).should().findAllByTrailingStopStatusInAndStockCodeIn(statuses.capture(), eq(List.of("005930", "000660")));

        List<TrailingStopStatus> expected = Arrays.stream(TrailingStopStatus.values()).filter(TrailingStopStatus::isOpen).toList();
        assertThat(statuses.getValue())
                .isNotEmpty()
                .containsExactlyElementsOf(expected)
                .allMatch(TrailingStopStatus::isOpen);
        assertThat(result).isSameAs(found);
    }
}
