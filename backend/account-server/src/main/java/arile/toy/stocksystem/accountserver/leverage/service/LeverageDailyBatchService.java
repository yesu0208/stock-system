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

    public void runDailyLeverageBatch() {

        List<LeveragePositionEntity> allPositions = leveragePositionRepository.findAll();

        int accrued = accrueInterestForAllPositions(allPositions);

        log.info("[LeverageDailyBatch] interest accrual completed. totalPositions={}, accrued={}",
                allPositions.size(), accrued);

        // TODO: 담보비율 재계산 + 마진콜 판정
        // TODO: 반대매매 실행
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
