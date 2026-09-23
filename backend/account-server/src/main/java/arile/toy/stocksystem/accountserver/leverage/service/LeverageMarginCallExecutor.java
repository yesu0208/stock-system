package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import arile.toy.stocksystem.accountserver.leverage.event.publisher.MarginCallEventPublisher;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 레버리지 포지션 1건의 담보비율 재계산 + 마진콜 판정 실행기.
 * LeverageMarginCallService의 배치 루프에서 호출되며, 포지션마다 개별 트랜잭션으로 실행
 * (배치 전체를 하나의 트랜잭션으로 묶으면 한 포지션의 DB 예외로 트랜잭션 전체가 rollback-only가 되어
 *  이미 처리된 다른 포지션의 상태 전이까지 롤백되므로 포지션 단위로 분리)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeverageMarginCallExecutor {

    private final LeveragePositionRepository leveragePositionRepository;
    private final StockSummaryRedisRepository stockSummaryRedisRepository;
    private final MarginRatioCalculator marginRatioCalculator;
    private final LeveragePositionRedisSyncer redisSyncer;
    private final AccountMarginStatusSyncer accountMarginStatusSyncer;
    private final MarginCallEventPublisher marginCallEventPublisher;

    public enum MarginCallOutcome {
        NEW_MARGIN_CALL,
        RECOVERED,
        QUEUED_FOR_LIQUIDATION,
        UNCHANGED
    }

    /**
     * 포지션 하나를 평가.
     * 배치 시작 시점에 읽은 포지션이 아니라 최신 포지션을 락으로 다시 읽어 판정.
     * NORMAL -> MARGIN_CALL -> (재평가) -> NORMAL 복귀 또는 LIQUIDATION_PENDING 전환.
     */
    @Transactional
    public MarginCallOutcome evaluateOnePosition(Long positionId, LocalDate today) {

        LeveragePositionEntity position = leveragePositionRepository.findByIdForUpdate(positionId)
                .orElseThrow(() -> new IllegalStateException("Leverage position not found. id=" + positionId));

        if (position.getMarginStatus() == MarginStatus.LIQUIDATION_PENDING) {
            return MarginCallOutcome.UNCHANGED; // 이미 청산 대기 중
        }

        if (position.getLoanAmount() <= 0) {
            if (position.getMarginStatus() != MarginStatus.NORMAL) {
                transitionToNormal(position);
                return MarginCallOutcome.RECOVERED;
            }
            return MarginCallOutcome.UNCHANGED;
        }

        Long curPrice = resolveCurrentPrice(position.getStockCode());
        if (curPrice == null) {
            log.warn("[MarginCall] No price found. skip evaluation. username={}, stockCode={}, leverageRatio={}",
                    position.getUsername(), position.getStockCode(), position.getLeverageRatio());
            return MarginCallOutcome.UNCHANGED;
        }

        long evaluationAmount = (long) position.getQuantity() * curPrice;
        double ratio = marginRatioCalculator.calculateRatio(evaluationAmount, position.getLoanAmount());
        boolean below = marginRatioCalculator.isBelowMaintenance(ratio);

        if (position.getMarginStatus() == MarginStatus.NORMAL) {
            if (below) {
                transitionToMarginCall(position, today, ratio);
                return MarginCallOutcome.NEW_MARGIN_CALL;
            }
            return MarginCallOutcome.UNCHANGED;
        }

        // MARGIN_CALL 상태 — 재평가 시점 (D+1 종가 기준)
        if (!below) {
            transitionToNormal(position);
            publishRecoveredEvent(position, ratio);
            return MarginCallOutcome.RECOVERED;
        }

        transitionToLiquidationPending(position, ratio);
        return MarginCallOutcome.QUEUED_FOR_LIQUIDATION;
    }

    private void transitionToMarginCall(LeveragePositionEntity position, LocalDate today, double ratio) {
        position.changeMarginStatus(MarginStatus.MARGIN_CALL, today);
        leveragePositionRepository.save(position);
        redisSyncer.sync(position);
        accountMarginStatusSyncer.resync(position.getUsername());

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
        accountMarginStatusSyncer.resync(position.getUsername());
    }

    private void transitionToLiquidationPending(LeveragePositionEntity position, double ratio) {
        position.changeMarginStatus(MarginStatus.LIQUIDATION_PENDING, position.getMarginCallDate());
        leveragePositionRepository.save(position);
        redisSyncer.sync(position);
        accountMarginStatusSyncer.resync(position.getUsername());

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
