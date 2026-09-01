package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageDailyBatchService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final LeverageInterestCalculator interestCalculator;
    private final LeveragePositionRedisSyncer redisSyncer;
    private final LeverageMarginCallService leverageMarginCallService;
    private final LeverageLiquidationService leverageLiquidationService;
    private final NegativeBalanceResolutionService negativeBalanceResolutionService;

    /**
     * 매일 15:55 실행되는 레버리지 배치의 전체 파이프라인
     * 이자누적 -> 담보비율재계산/마진콜판정 -> 반대매매(청산) -> ④마이너스계좌 유예판정/영구정지
     */
    public void runDailyLeverageBatch() {

        List<LeveragePositionEntity> allPositions = leveragePositionRepository.findAll();

        int accrued = accrueInterestForAllPositions(allPositions);
        log.info("[LeverageDailyBatch] interest accrual completed. totalPositions={}, accrued={}",
                allPositions.size(), accrued);

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

    @Transactional
    public int accrueInterestForAllPositions(List<LeveragePositionEntity> positions) {

        int accrued = 0;

        for (LeveragePositionEntity position : positions) {

            if (position.getLoanAmount() <= 0) {
                continue;
            }

            try {
                long dailyInterest = interestCalculator.calculateDailyInterest(position.getLoanAmount());

                if (dailyInterest <= 0) {
                    continue;
                }

                position.accrueInterest(dailyInterest);
                leveragePositionRepository.save(position);
                redisSyncer.sync(position);

                accrued++;

            } catch (Exception e) {
                log.error("[LeverageDailyBatch] interest accrual failed for positionId={}, username={}, stockCode={}",
                        position.getLeveragePositionId(), position.getUsername(), position.getStockCode(), e);
            }
        }

        return accrued;
    }
}
