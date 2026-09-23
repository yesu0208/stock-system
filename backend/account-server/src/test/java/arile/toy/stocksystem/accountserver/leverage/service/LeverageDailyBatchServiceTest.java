package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginCallBatchResult;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageLiquidationService.LiquidationBatchResult;
import arile.toy.stocksystem.accountserver.leverage.service.NegativeBalanceResolutionService.ResolutionBatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageDailyBatchServiceTest {

    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private LeverageInterestService leverageInterestService;
    @Mock private LeverageMarginCallService leverageMarginCallService;
    @Mock private LeverageLiquidationService leverageLiquidationService;
    @Mock private NegativeBalanceResolutionService negativeBalanceResolutionService;

    @InjectMocks
    private LeverageDailyBatchService service;

    private static LeveragePositionEntity position(String stockCode) {
        return LeveragePositionEntity.of("user1", stockCode, LeverageRatio.X2, 10, 700_000L, 700_100L);
    }

    @Test
    @DisplayName("이자 청구 → 마진콜 판정 → 청산 대상 재조회 → 청산 → 마이너스 계좌 판정 순서로 실행한다")
    void runsPipelineInOrder() {
        List<LeveragePositionEntity> allPositions = List.of(position("005930"), position("000660"));
        List<LeveragePositionEntity> pending = List.of(position("005930"));

        given(leveragePositionRepository.findAll()).willReturn(allPositions);
        given(leverageInterestService.chargeInterestForAllPositions(allPositions)).willReturn(2);
        given(leverageMarginCallService.evaluatePositions(allPositions))
                .willReturn(new MarginCallBatchResult(1, 0, 1));
        given(leveragePositionRepository.findByMarginStatus(MarginStatus.LIQUIDATION_PENDING)).willReturn(pending);
        given(leverageLiquidationService.liquidatePendingPositions(pending))
                .willReturn(new LiquidationBatchResult(1, 0));
        given(negativeBalanceResolutionService.resolveNegativeAccounts())
                .willReturn(new ResolutionBatchResult(0, 0));

        service.runDailyLeverageBatch();

        InOrder inOrder = inOrder(leveragePositionRepository, leverageInterestService, leverageMarginCallService,
                leverageLiquidationService, negativeBalanceResolutionService);
        inOrder.verify(leveragePositionRepository).findAll();
        inOrder.verify(leverageInterestService).chargeInterestForAllPositions(allPositions);
        inOrder.verify(leverageMarginCallService).evaluatePositions(allPositions);
        // 마진콜 판정에서 새로 LIQUIDATION_PENDING이 된 포지션까지 포함하도록 판정 이후에 재조회
        inOrder.verify(leveragePositionRepository).findByMarginStatus(MarginStatus.LIQUIDATION_PENDING);
        inOrder.verify(leverageLiquidationService).liquidatePendingPositions(pending);
        inOrder.verify(negativeBalanceResolutionService).resolveNegativeAccounts();
    }

    @Test
    @DisplayName("중간 단계에서 예외가 나면 이후 단계를 실행하지 않고 예외를 전파한다")
    void whenStepFails_stopsAndPropagates() {
        List<LeveragePositionEntity> allPositions = List.of(position("005930"));
        given(leveragePositionRepository.findAll()).willReturn(allPositions);
        given(leverageInterestService.chargeInterestForAllPositions(allPositions)).willReturn(1);
        given(leverageMarginCallService.evaluatePositions(allPositions))
                .willThrow(new IllegalStateException("margin call failed"));

        assertThatThrownBy(() -> service.runDailyLeverageBatch())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("margin call failed");

        verifyNoInteractions(leverageLiquidationService, negativeBalanceResolutionService);
    }
}
