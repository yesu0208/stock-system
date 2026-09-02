package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageDailyBatchService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final LeverageInterestService leverageInterestService;
    private final LeverageMarginCallService leverageMarginCallService;
    private final LeverageLiquidationService leverageLiquidationService;
    private final NegativeBalanceResolutionService negativeBalanceResolutionService;

    /**
     * 매일 15:55 실행되는 레버리지 배치의 전체 파이프라인
     * 이자누적 -> 담보비율재계산/마진콜판정 -> 반대매매(청산) -> ④마이너스계좌 유예판정/영구정지
     */
    public void runDailyLeverageBatch() {

        List<LeveragePositionEntity> allPositions = leveragePositionRepository.findAll();

        int charged = leverageInterestService.chargeInterestForAllPositions(allPositions);
        log.info("[LeverageDailyBatch] interest charge completed. totalPositions={}, charged={}",
                allPositions.size(), charged);

        var marginCallResult = leverageMarginCallService.evaluatePositions(allPositions);
        log.info("[LeverageDailyBatch] margin call evaluation completed. newMarginCalls={}, recovered={}, queuedForLiquidation={}",
                marginCallResult.newMarginCalls(), marginCallResult.recovered(), marginCallResult.queuedForLiquidation());

        List<LeveragePositionEntity> pendingLiquidations =
                leveragePositionRepository.findByMarginStatus(MarginStatus.LIQUIDATION_PENDING);

        var liquidationResult = leverageLiquidationService.liquidatePendingPositions(pendingLiquidations);
        log.info("[LeverageDailyBatch] liquidation completed. liquidated={}, withShortfall={}",
                liquidationResult.liquidated(), liquidationResult.shortfallCount());

        var resolutionResult = negativeBalanceResolutionService.resolveNegativeAccounts();
        log.info("[LeverageDailyBatch] negative balance resolution completed. recovered={}, suspended={}",
                resolutionResult.recovered(), resolutionResult.suspended());

        log.info("[LeverageDailyBatch] all steps completed.");
    }
}
