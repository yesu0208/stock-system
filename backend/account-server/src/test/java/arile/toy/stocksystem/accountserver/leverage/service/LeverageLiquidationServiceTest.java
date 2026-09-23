package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageLiquidationService.LiquidationBatchResult;
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
class LeverageLiquidationServiceTest {

    @Mock
    private LeverageLiquidationExecutor leverageLiquidationExecutor;

    @InjectMocks
    private LeverageLiquidationService service;

    private static LeveragePositionEntity position(long id, MarginStatus status) {
        LeveragePositionEntity position =
                LeveragePositionEntity.of("user1", "005930", LeverageRatio.X2, 10, 700_000L, 700_100L);
        position.changeMarginStatus(status, LocalDate.now().minusDays(1));
        ReflectionTestUtils.setField(position, "leveragePositionId", id);
        return position;
    }

    @Test
    @DisplayName("LIQUIDATION_PENDING이 아닌 포지션은 청산을 호출하지 않는다")
    void skipsNonPending() {
        List<LeveragePositionEntity> positions = List.of(
                position(1L, MarginStatus.NORMAL),
                position(2L, MarginStatus.MARGIN_CALL));

        LiquidationBatchResult result = service.liquidatePendingPositions(positions);

        assertThat(result).isEqualTo(new LiquidationBatchResult(0, 0));
        verifyNoInteractions(leverageLiquidationExecutor);
    }

    @Test
    @DisplayName("청산 건수와 부족분 발생 건수를 집계하고, 실패한 포지션은 집계에서 제외한 채 계속 진행한다")
    void aggregatesAndContinuesOnFailure() {
        given(leverageLiquidationExecutor.liquidateOnePosition(1L)).willReturn(false);
        given(leverageLiquidationExecutor.liquidateOnePosition(2L)).willReturn(true);
        given(leverageLiquidationExecutor.liquidateOnePosition(3L))
                .willThrow(new IllegalStateException("Leverage position not found. id=3"));
        given(leverageLiquidationExecutor.liquidateOnePosition(4L)).willReturn(true);

        LiquidationBatchResult result = service.liquidatePendingPositions(List.of(
                position(1L, MarginStatus.LIQUIDATION_PENDING),
                position(2L, MarginStatus.LIQUIDATION_PENDING),
                position(3L, MarginStatus.LIQUIDATION_PENDING),
                position(4L, MarginStatus.LIQUIDATION_PENDING)));

        assertThat(result).isEqualTo(new LiquidationBatchResult(3, 2));
        verify(leverageLiquidationExecutor).liquidateOnePosition(4L);
    }
}
