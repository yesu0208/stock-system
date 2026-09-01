package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeverageLiquidationEntity;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.event.LiquidationExecutedEvent;
import arile.toy.stocksystem.accountserver.leverage.event.publisher.LiquidationEventPublisher;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageLiquidationRepository;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageLiquidationService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final LeverageLiquidationRepository leverageLiquidationRepository;
    private final UserAccountRepository userAccountRepository;
    private final StockSummaryRedisRepository stockSummaryRedisRepository;
    private final LeveragePositionRedisSyncer redisSyncer;
    private final LiquidationEventPublisher liquidationEventPublisher;

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
                boolean hadShortfall = liquidateOnePosition(position.getLeveragePositionId());
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

    /**
     * 포지션 하나를 청산한다. 계좌 락과 포지션 락을 함께 잡기 위해 트랜잭션을 개별 분리했다
     * (배치 전체를 하나의 트랜잭션으로 묶으면 한 유저의 락이 배치 전체를 지연시킬 수 있음).
     *
     * @return 부족분(shortfall)이 발생했는지 여부
     */
    @Transactional
    public boolean liquidateOnePosition(Long positionId) {

        LeveragePositionEntity position = leveragePositionRepository.findByIdForUpdate(positionId)
                .orElseThrow(() -> new IllegalStateException("Leverage position not found. id=" + positionId));

        if (position.getMarginStatus() != MarginStatus.LIQUIDATION_PENDING) {
            // 배치 실행 사이 동시성 이슈로 이미 처리되었거나 상태가 바뀐 경우 — 안전하게 스킵
            return false;
        }

        Long settlementPrice = resolveCurrentPrice(position.getStockCode());
        if (settlementPrice == null) {
            // 가격을 못 구하면 이번 배치에서는 청산을 건너뛴다. 다음 배치(다음 거래일)에 재시도된다.
            log.error("[Liquidation] No price available. Deferring to next batch. username={}, stockCode={}",
                    position.getUsername(), position.getStockCode());
            return false;
        }

        int quantity = position.getQuantity();
        long proceeds = quantity * settlementPrice;
        long loanAmount = position.getLoanAmount();

        long netAfterRepay = proceeds - loanAmount;
        long shortfall = netAfterRepay < 0 ? -netAfterRepay : 0L;

        UserAccountEntity account = userAccountRepository.findByUsernameForUpdate(position.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. username=" + position.getUsername()));

        // netAfterRepay가 음수여도 그대로 반영 — 계좌가 마이너스로 전환되는 것이 의도된 동작
        account.setBalance(account.getBalance() + netAfterRepay);

        if (shortfall > 0 && account.getAccountStatus() == AccountStatus.NORMAL) {
            account.changeAccountStatus(AccountStatus.NEGATIVE, LocalDate.now());
            log.warn("[Liquidation] Account converted to NEGATIVE. username={}, shortfall={}",
                    position.getUsername(), shortfall);
        }

        userAccountRepository.save(account);

        leverageLiquidationRepository.save(
                LeverageLiquidationEntity.of(
                        position.getUsername(), position.getStockCode(), position.getLeverageRatio(),
                        quantity, settlementPrice, proceeds, loanAmount, shortfall)
        );

        String username = position.getUsername();
        String stockCode = position.getStockCode();
        var leverageRatio = position.getLeverageRatio();

        leveragePositionRepository.delete(position);
        redisSyncer.remove(username, stockCode, leverageRatio);

        liquidationEventPublisher.publish(
                LiquidationExecutedEvent.of(username, stockCode, leverageRatio, quantity, settlementPrice, shortfall));

        log.warn("[Liquidation] executed. username={}, stockCode={}, leverageRatio={}, quantity={}, " +
                        "settlementPrice={}, proceeds={}, repaidLoan={}, shortfall={}",
                username, stockCode, leverageRatio, quantity, settlementPrice, proceeds, loanAmount, shortfall);

        return shortfall > 0;
    }

    private Long resolveCurrentPrice(String stockCode) {
        var summary = stockSummaryRedisRepository.findByStockCode(stockCode);
        if (summary == null || summary.curPrice() == null) {
            return null;
        }
        return summary.curPrice().longValue();
    }

    public record LiquidationBatchResult(int liquidated, int shortfallCount) {
    }
}
