package arile.toy.stocksystem.accountserver.leverage.service;

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

    public void runDailyLeverageBatch() {

        List<LeveragePositionEntity> allPositions = leveragePositionRepository.findAll();

        int accrued = accrueInterestForAllPositions(allPositions);
        log.info("[LeverageDailyBatch] interest accrual completed. totalPositions={}, accrued={}",
                allPositions.size(), accrued);

        var marginCallResult = leverageMarginCallService.evaluatePositions(allPositions);
        log.info("[LeverageDailyBatch] margin call evaluation completed. newMarginCalls={}, recovered={}, queuedForLiquidation={}",
                marginCallResult.newMarginCalls(), marginCallResult.recovered(), marginCallResult.queuedForLiquidation());

        // evaluatePositions에서 이번 배치에 LIQUIDATION_PENDING으로 새로 전환된 것만 청산하면 되므로
        // 이전 배치에서 밀린 것까지 포함해 전체 목록을 다시 조회한다 (가격 조회 실패로 연기된 건 포함)
        List<LeveragePositionEntity> pendingLiquidations = leveragePositionRepository
                .findByMarginStatus(arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus.LIQUIDATION_PENDING);

        var liquidationResult = leverageLiquidationService.liquidatePendingPositions(pendingLiquidations);
        log.info("[LeverageDailyBatch] liquidation completed. liquidated={}, withShortfall={}",
                liquidationResult.liquidated(), liquidationResult.shortfallCount());

        // TODO: 마이너스 계좌 유예 만료 판정 + 영구정지 전환
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
