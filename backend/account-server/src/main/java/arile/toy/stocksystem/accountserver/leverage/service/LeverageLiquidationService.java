package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageLiquidationService {

    private final LeverageLiquidationExecutor leverageLiquidationExecutor;

    /**
     * 반대매매~청산상환+부족분 마이너스 전환 단계.
     * LIQUIDATION_PENDING 상태의 포지션 전량을 당일 종가(마진콜 판정과 동일 기준)로 강제 청산
     * 전액상환방식: 부분 청산 없이 포지션 전체 수량을 매도 처리
     */
    public LiquidationBatchResult liquidatePendingPositions(List<LeveragePositionEntity> positions) {

        int liquidated = 0;
        int shortfallCount = 0;

        for (LeveragePositionEntity position : positions) {

            if (position.getMarginStatus() != MarginStatus.LIQUIDATION_PENDING) {
                continue;
            }

            try {
                boolean hadShortfall = leverageLiquidationExecutor.liquidateOnePosition(position.getLeveragePositionId());
                liquidated++;
                if (hadShortfall) {
                    shortfallCount++;
                }
            } catch (Exception e) {
                log.error("[Liquidation] failed. positionId={}, username={}, stockCode={}",
                        position.getLeveragePositionId(), position.getUsername(), position.getStockCode(), e);
            }
        }

        log.info("[Liquidation] batch completed. liquidated={}, withShortfall={}", liquidated, shortfallCount);

        return new LiquidationBatchResult(liquidated, shortfallCount);
    }

    public record LiquidationBatchResult(int liquidated, int shortfallCount) {
    }
}
