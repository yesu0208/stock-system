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

    public void runDailyLeverageBatch() {

        List<LeveragePositionEntity> allPositions = leveragePositionRepository.findAll();

        int accrued = accrueInterestForAllPositions(allPositions);
        log.info("[LeverageDailyBatch] interest accrual completed. totalPositions={}, accrued={}",
                allPositions.size(), accrued);

        // 이자 누적으로 loanAmount가 갱신되었으므로, 담보비율 재계산은 갱신된 엔티티를 다시 사용해야 함
        var marginCallResult = leverageMarginCallService.evaluatePositions(allPositions);
        log.info("[LeverageDailyBatch] margin call evaluation completed. newMarginCalls={}, recovered={}, queuedForLiquidation={}",
                marginCallResult.newMarginCalls(), marginCallResult.recovered(), marginCallResult.queuedForLiquidation());

        // TODO: LIQUIDATION_PENDING 포지션 반대매매 실행
        // TODO: 마이너스 계좌 처리
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
