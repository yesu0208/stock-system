package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.trade.service.TradeCostCalculator;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeveragePositionApplyService {

    private final UserAccountRepository userAccountRepository;
    private final LeveragePositionRepository leveragePositionRepository;
    private final LeveragePositionRedisSyncer redisSyncer;
    private final AccountMarginStatusSyncer accountMarginStatusSyncer;
    private final AccountBalanceCommand accountBalanceCommand;
    private final TradeCostCalculator tradeCostCalculator;

    /**
     * 레버리지 매수 체결 반영.
     * 계좌에서는 "개시증거금"만 차감한다 (매수금액 전체가 아님 — 나머지는 대출금으로 처리).
     * 매수 주문 시 이미 reserveCash로 증거금만 예약되어 있었으므로, 여기서는 그 예약분을 실제 확정 차감한다.
     */
    @Transactional
    public void applyLeverageBuy(TradeExecutedEvent event, LeverageRatio leverageRatio) {

        int executable = event.tradeQuantity();
        long tradeAmount = (long) event.tradePrice() * executable;

        // 재계산(calculateMarginDeposit) 대신, stock-server가 OrderEntity의
        // remainingReservedMargin에서 정확히 비례 배분해 보낸 값을 그대로 사용
        long orderMarginAmount = event.reservedMarginConsumed() != null ? event.reservedMarginConsumed() : 0L;
        long tradeMarginAmount = leverageRatio.calculateMarginDeposit(tradeAmount);
        long marginRefund = orderMarginAmount - tradeMarginAmount;

        // 재계산(tradeCostCalculator.calculateFee) 대신, stock-server가 OrderEntity의
        // remainingReservedFee에서 정확히 비례 배분해 보낸 값을 그대로 사용
        long orderAmount = (long) event.orderPrice() * executable;
        long feeReserved = event.reservedFeeConsumed() != null ? event.reservedFeeConsumed() : 0L;
        long feeActual = tradeCostCalculator.calculateFee(tradeAmount);
        long feeRefund = feeReserved - feeActual;

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(event.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getBalance() < tradeMarginAmount + feeActual) {
            throw new IllegalStateException(
                    "DB/Redis balance inconsistency detected during leverage trade apply.");
        }

        account.setBalance(account.getBalance() - tradeMarginAmount - feeActual);
        userAccountRepository.save(account);

        LeveragePositionEntity position = leveragePositionRepository
                .findByUsernameAndStockCodeAndLeverageRatioForUpdate(event.username(), event.stockCode(), leverageRatio)
                .orElseGet(() -> LeveragePositionEntity.of(event.username(), event.stockCode(), leverageRatio, 0, 0L, 0L));

        long additionalLoanAmount = leverageRatio.calculateLoanAmount(tradeAmount);
        position.addPurchase(executable, tradeAmount, tradeAmount + feeActual, additionalLoanAmount);
        leveragePositionRepository.save(position);

        redisSyncer.sync(position);

        // 해제할 예약분에 feeReserved 포함, 환급할 차액에 feeRefund 포함
        boolean settled = accountBalanceCommand.settleLeverageBuy(
                event.username(), orderMarginAmount + feeReserved, marginRefund + feeRefund);
        if (!settled) {
            log.error("Redis reservedCash settlement failed for leverage buy. username={}, stockCode={}, orderMarginAmount={}",
                    event.username(), event.stockCode(), orderMarginAmount);
            throw new IllegalStateException(
                    "Redis leverage buy settlement failed. username=%s, stockCode=%s"
                            .formatted(event.username(), event.stockCode()));
        }

        log.info("Leverage buy applied. username={}, stockCode={}, leverageRatio={}, tradeAmount={}, marginCharged={}, feeCharged={}",
                event.username(), event.stockCode(), leverageRatio, tradeAmount, tradeMarginAmount, feeActual);
    }

    /**
     * 레버리지 매도 체결 반영.
     * 매도 대금에서 대출금 비례 상환분을 먼저 차감하고, 나머지(순수익)만 계좌 현금으로 반환한다.
     */
    @Transactional
    public void applyLeverageSell(TradeExecutedEvent event, LeverageRatio leverageRatio) {

        int executable = event.tradeQuantity();
        long tradeAmount = (long) event.tradePrice() * executable; // 매도 대금

        // 수수료+거래세, 레버리지도 매도금액 전체(포지션 전체 크기) 기준으로 동일 적용
        long fee = tradeCostCalculator.calculateFee(tradeAmount);
        long tax = tradeCostCalculator.calculateTax(tradeAmount);
        long totalCost = fee + tax;

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(event.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        LeveragePositionEntity position = leveragePositionRepository
                .findByUsernameAndStockCodeAndLeverageRatioForUpdate(event.username(), event.stockCode(), leverageRatio)
                .orElseThrow(() -> new IllegalStateException("Leverage position not found."));

        if (position.getQuantity() < executable) {
            throw new IllegalStateException(
                    "DB/Redis leverage position quantity inconsistency detected during trade apply.");
        }

        boolean isFullLiquidation = position.getQuantity() == executable;

        long repaidLoanAmount = position.reduceBySell(executable);
        if (isFullLiquidation) {
            repaidLoanAmount += position.getLoanAmount();
            position.setLoanAmount(0L);
        }

        // 매도 대금 중 대출 상환분을 제외한 나머지가 유저에게 귀속되는 순수익
        long netProceeds = tradeAmount - repaidLoanAmount - totalCost; // 수수료+세금도 차감

        account.setBalance(account.getBalance() + netProceeds);
        userAccountRepository.save(account);

        if (position.isEmpty()) {
            leveragePositionRepository.delete(position);
            redisSyncer.remove(event.username(), event.stockCode(), leverageRatio);
            accountMarginStatusSyncer.resync(event.username());
        } else {
            leveragePositionRepository.save(position);
            redisSyncer.sync(position);
        }

        boolean credited = accountBalanceCommand.creditAvailableCash(event.username(), netProceeds);
        if (!credited) {
            log.error("Redis availableCash credit failed for leverage sell. username={}, stockCode={}, netProceeds={}",
                    event.username(), event.stockCode(), netProceeds);
            throw new IllegalStateException(
                    "Redis leverage sell credit failed. username=%s, stockCode=%s"
                            .formatted(event.username(), event.stockCode()));
        }

        log.info("Leverage sell applied. username={}, stockCode={}, leverageRatio={}, tradeAmount={}, " +
                        "repaidLoan={}, feeAndTax={}, netProceeds={}, positionRemaining={}",
                event.username(), event.stockCode(), leverageRatio, tradeAmount, repaidLoanAmount, totalCost, netProceeds,
                position.getQuantity());
    }

    /** 매도 주문 접수 시 availableQuantity만 선차감 (체결 전 예약) — reserveLeverageStock에서 호출 */
    @Transactional
    public boolean reserveLeverageStock(String username, String stockCode, LeverageRatio leverageRatio, int quantity) {

        LeveragePositionEntity position = leveragePositionRepository
                .findByUsernameAndStockCodeAndLeverageRatioForUpdate(username, stockCode, leverageRatio)
                .orElse(null);

        if (position == null || position.getAvailableQuantity() < quantity) {
            return false;
        }

        // 청산 확정 포지션은 유저 매도 불가
        if (position.getMarginStatus() == MarginStatus.LIQUIDATION_PENDING) {
            return false;
        }

        position.setAvailableQuantity(position.getAvailableQuantity() - quantity);
        leveragePositionRepository.save(position);

        redisSyncer.sync(position);
        return true;
    }

    @Transactional
    public boolean refundReservedLeverageStock(String username, String stockCode, LeverageRatio leverageRatio, int quantity) {

        LeveragePositionEntity position = leveragePositionRepository
                .findByUsernameAndStockCodeAndLeverageRatioForUpdate(username, stockCode, leverageRatio)
                .orElse(null);

        if (position == null) {
            return false;
        }

        position.setAvailableQuantity(position.getAvailableQuantity() + quantity);
        leveragePositionRepository.save(position);

        redisSyncer.sync(position);
        return true;
    }
}
