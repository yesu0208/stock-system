package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginCallBatchResult;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageMarginCallExecutor.MarginCallOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageMarginCallService {

    private final LeverageMarginCallExecutor leverageMarginCallExecutor;

    /**
     * 담보비율 재계산 + 마진콜 판정/재평가 단계.
     * 각 포지션은 이 메서드 1회 호출(=1 트레이딩데이)당 정확히 한 번만 평가된다.
     * NORMAL -> MARGIN_CALL -> (재평가) -> NORMAL 복귀 또는 LIQUIDATION_PENDING 전환.
     *
     * 트랜잭션은 포지션 단위로 LeverageMarginCallExecutor에서 개별 적용
     * (이 메서드에 @Transactional을 두면 실행기 트랜잭션이 합류해 포지션 단위 분리가 무의미해짐)
     */
    public MarginCallBatchResult evaluatePositions(List<LeveragePositionEntity> positions) {

        int newMarginCalls = 0;
        int recovered = 0;
        int queuedForLiquidation = 0;
        LocalDate today = LocalDate.now();

        for (LeveragePositionEntity position : positions) {

            try {
                MarginCallOutcome outcome =
                        leverageMarginCallExecutor.evaluateOnePosition(position.getLeveragePositionId(), today);

                switch (outcome) {
                    case NEW_MARGIN_CALL -> newMarginCalls++;
                    case RECOVERED -> recovered++;
                    case QUEUED_FOR_LIQUIDATION -> queuedForLiquidation++;
                    case UNCHANGED -> { }
                }

            } catch (Exception e) {
                log.error("[MarginCall] evaluation failed. positionId={}, username={}, stockCode={}",
                        position.getLeveragePositionId(), position.getUsername(), position.getStockCode(), e);
            }
        }

        log.info("[MarginCall] batch completed. total={}, newMarginCalls={}, recovered={}, queuedForLiquidation={}",
                positions.size(), newMarginCalls, recovered, queuedForLiquidation);

        return new MarginCallBatchResult(newMarginCalls, recovered, queuedForLiquidation);
    }
}
