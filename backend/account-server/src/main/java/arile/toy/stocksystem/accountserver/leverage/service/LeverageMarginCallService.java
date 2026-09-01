package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginCallBatchResult;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import arile.toy.stocksystem.accountserver.leverage.event.publisher.MarginCallEventPublisher;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageMarginCallService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final StockSummaryRedisRepository stockSummaryRedisRepository;
    private final MarginRatioCalculator marginRatioCalculator;
    private final LeveragePositionRedisSyncer redisSyncer;
    private final MarginCallEventPublisher marginCallEventPublisher;

    /**
     * 담보비율 재계산 + 마진콜 판정/재평가 단계.
     * 각 포지션은 이 메서드 1회 호출(=1 트레이딩데이)당 정확히 한 번만 평가된다.
     * NORMAL -> MARGIN_CALL -> (재평가) -> NORMAL 복귀 또는 LIQUIDATION_PENDING 전환.
     */
    @Transactional
    public MarginCallBatchResult evaluatePositions(List<LeveragePositionEntity> positions) {

        int newMarginCalls = 0;
        int recovered = 0;
        int queuedForLiquidation = 0;
        LocalDate today = LocalDate.now();

        for (LeveragePositionEntity position : positions) {

            try {
                if (position.getMarginStatus() == MarginStatus.LIQUIDATION_PENDING) {
                    continue; // 이미 청산 대기 중
                }

                if (position.getLoanAmount() <= 0) {
                    if (position.getMarginStatus() != MarginStatus.NORMAL) {
                        transitionToNormal(position);
                        recovered++;
                    }
                    continue;
                }

                Long curPrice = resolveCurrentPrice(position.getStockCode());
                if (curPrice == null) {
                    log.warn("[MarginCall] No price found. skip evaluation. username={}, stockCode={}, leverageRatio={}",
                            position.getUsername(), position.getStockCode(), position.getLeverageRatio());
                    continue;
                }

                long evaluationAmount = (long) position.getQuantity() * curPrice;
                double ratio = marginRatioCalculator.calculateRatio(evaluationAmount, position.getLoanAmount());
                boolean below = marginRatioCalculator.isBelowMaintenance(ratio);

                if (position.getMarginStatus() == MarginStatus.NORMAL) {
                    if (below) {
                        transitionToMarginCall(position, today, ratio);
                        newMarginCalls++;
                    }
                } else { // MARGIN_CALL 상태 — 재평가 시점 (D+1 종가 기준)
                    if (!below) {
                        transitionToNormal(position);
                        publishRecoveredEvent(position, ratio);
                        recovered++;
                    } else {
                        transitionToLiquidationPending(position, ratio);
                        queuedForLiquidation++;
                    }
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

    private void transitionToMarginCall(LeveragePositionEntity position, LocalDate today, double ratio) {
        position.changeMarginStatus(MarginStatus.MARGIN_CALL, today);
        leveragePositionRepository.save(position);
        redisSyncer.sync(position);

        marginCallEventPublisher.publish(
                MarginCallEvent.of(position.getUsername(), position.getStockCode(),
                        position.getLeverageRatio(), MarginStatus.MARGIN_CALL, ratio));

        log.warn("[MarginCall] triggered. username={}, stockCode={}, leverageRatio={}, ratio={}",
                position.getUsername(), position.getStockCode(), position.getLeverageRatio(), ratio);
    }

    private void transitionToNormal(LeveragePositionEntity position) {
        position.changeMarginStatus(MarginStatus.NORMAL, null);
        leveragePositionRepository.save(position);
        redisSyncer.sync(position);
    }

    private void transitionToLiquidationPending(LeveragePositionEntity position, double ratio) {
        // marginCallDate(D일)는 감사(audit) 기록용으로 그대로 보존
        position.changeMarginStatus(MarginStatus.LIQUIDATION_PENDING, position.getMarginCallDate());
        leveragePositionRepository.save(position);
        redisSyncer.sync(position);

        marginCallEventPublisher.publish(
                MarginCallEvent.of(position.getUsername(), position.getStockCode(),
                        position.getLeverageRatio(), MarginStatus.LIQUIDATION_PENDING, ratio));

        log.warn("[MarginCall] grace expired, queued for liquidation. username={}, stockCode={}, leverageRatio={}, ratio={}",
                position.getUsername(), position.getStockCode(), position.getLeverageRatio(), ratio);
    }

    private void publishRecoveredEvent(LeveragePositionEntity position, double ratio) {
        marginCallEventPublisher.publish(
                MarginCallEvent.of(position.getUsername(), position.getStockCode(),
                        position.getLeverageRatio(), MarginStatus.NORMAL, ratio));
    }

    private Long resolveCurrentPrice(String stockCode) {
        var summary = stockSummaryRedisRepository.findByStockCode(stockCode);
        if (summary == null || summary.curPrice() == null) {
            return null;
        }
        return summary.curPrice().longValue();
    }
}
