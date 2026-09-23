package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginCallBatchResult;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageMarginCallExecutor.MarginCallOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageMarginCallServiceTest {

    @Mock
    private LeverageMarginCallExecutor leverageMarginCallExecutor;

    @InjectMocks
    private LeverageMarginCallService service;

    private static LeveragePositionEntity position(long id) {
        LeveragePositionEntity position =
                LeveragePositionEntity.of("user1", "00000" + id, LeverageRatio.X2, 10, 700_000L, 700_100L);
        ReflectionTestUtils.setField(position, "leveragePositionId", id);
        return position;
    }

    @Test
    @DisplayName("포지션이 없으면 평가하지 않고 0건으로 집계한다")
    void whenNoPositions_returnsZero() {
        assertThat(service.evaluatePositions(List.of())).isEqualTo(new MarginCallBatchResult(0, 0, 0));

        verifyNoInteractions(leverageMarginCallExecutor);
    }

    @Test
    @DisplayName("포지션별 판정 결과를 집계하고, 실패한 포지션은 집계에서 제외한 채 계속 평가한다")
    void aggregatesAndContinuesOnFailure() {
        LocalDate today = LocalDate.now();
        given(leverageMarginCallExecutor.evaluateOnePosition(1L, today)).willReturn(MarginCallOutcome.NEW_MARGIN_CALL);
        given(leverageMarginCallExecutor.evaluateOnePosition(2L, today)).willReturn(MarginCallOutcome.RECOVERED);
        given(leverageMarginCallExecutor.evaluateOnePosition(3L, today)).willReturn(MarginCallOutcome.QUEUED_FOR_LIQUIDATION);
        given(leverageMarginCallExecutor.evaluateOnePosition(4L, today)).willReturn(MarginCallOutcome.UNCHANGED);
        given(leverageMarginCallExecutor.evaluateOnePosition(5L, today))
                .willThrow(new IllegalStateException("db down"));
        given(leverageMarginCallExecutor.evaluateOnePosition(6L, today)).willReturn(MarginCallOutcome.NEW_MARGIN_CALL);

        MarginCallBatchResult result = service.evaluatePositions(List.of(
                position(1L), position(2L), position(3L), position(4L), position(5L), position(6L)));

        assertThat(result).isEqualTo(new MarginCallBatchResult(2, 1, 1));
        verify(leverageMarginCallExecutor).evaluateOnePosition(6L, today);
    }
}
