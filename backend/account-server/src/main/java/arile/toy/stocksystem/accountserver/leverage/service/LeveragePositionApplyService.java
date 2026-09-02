package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
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
    private final AccountBalanceCommand accountBalanceCommand;

    /**
     * 레버리지 매수 체결 반영.
     * 계좌에서는 "개시증거금"만 차감한다 (매수금액 전체가 아님 — 나머지는 대출금으로 처리).
     * 매수 주문 시 이미 reserveCash로 증거금만 예약되어 있었으므로, 여기서는 그 예약분을 실제 확정 차감한다.
     */
    @Transactional
    public void applyLeverageBuy(TradeExecutedEvent event, LeverageRatio leverageRatio) {

        int executable = event.tradeQuantity();
        long tradeAmount = (long) event.tradePrice() * executable;          // 실제 체결된 매수금액(포지션 전체 크기)
        long orderMarginAmount = leverageRatio.calculateMarginDeposit((long) event.orderPrice() * executable); // 예약 당시 증거금
        long tradeMarginAmount = leverageRatio.calculateMarginDeposit(tradeAmount); // 체결가 기준 증거금
        long marginRefund = orderMarginAmount - tradeMarginAmount;      // 지정가보다 유리하게 체결된 경우 환급할 증거금 차액

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(event.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getBalance() < tradeMarginAmount) {
            throw new IllegalStateException(
                    "DB/Redis balance inconsistency detected during leverage trade apply.");
        }

        account.setBalance(account.getBalance() - tradeMarginAmount);
        userAccountRepository.save(account);

        LeveragePositionEntity position = leveragePositionRepository
                .findByUsernameAndStockCodeAndLeverageRatioForUpdate(event.username(), event.stockCode(), leverageRatio)
                .orElseGet(() -> LeveragePositionEntity.of(event.username(), event.stockCode(), leverageRatio, 0, 0L));

        long additionalLoanAmount = leverageRatio.calculateLoanAmount(tradeAmount);
        position.addPurchase(executable, tradeAmount, additionalLoanAmount);
        leveragePositionRepository.save(position);

        redisSyncer.sync(position);

        // reservedCash에서 예약분(orderMarginAmount) 전체를 해제하고, 그중 차액(marginRefund)만 availableCash로 반환한다.
        // marginRefund == 0(지정가 그대로 체결)이어도 반드시 호출해야 한다 — reservedCash에 tradeMarginAmount가
        // 영구히 남는 것을 막기 위함 (DB balance는 이미 tradeMarginAmount만큼만 차감되었으므로 그만큼은 Redis에서도 소멸해야 함).
        boolean settled = accountBalanceCommand.settleLeverageBuy(event.username(), orderMarginAmount, marginRefund);
        if (!settled) {
            log.error("Redis reservedCash settlement failed for leverage buy. username={}, stockCode={}, orderMarginAmount={}",
                    event.username(), event.stockCode(), orderMarginAmount);
            throw new IllegalStateException(
                    "Redis leverage buy settlement failed. username=%s, stockCode=%s"
                            .formatted(event.username(), event.stockCode()));
        }

        log.info("Leverage buy applied. username={}, stockCode={}, leverageRatio={}, tradeAmount={}, marginCharged={}",
                event.username(), event.stockCode(), leverageRatio, tradeAmount, tradeMarginAmount);
    }

    /**
     * 레버리지 매도 체결 반영.
     * 매도 대금에서 대출금 비례 상환분을 먼저 차감하고, 나머지(순수익)만 계좌 현금으로 반환한다.
     */
    @Transactional
    public void applyLeverageSell(TradeExecutedEvent event, LeverageRatio leverageRatio) {

        int executable = event.tradeQuantity();
        long tradeAmount = (long) event.tradePrice() * executable; // 매도 대금

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
            // 정수 나눗셈 오차로 loanAmount가 완전히 0이 안 될 수 있으므로 전량매도 시 명시적으로 0 처리
            repaidLoanAmount += position.getLoanAmount();
            position.setLoanAmount(0L);
        }

        // 매도 대금 중 대출 상환분을 제외한 나머지가 유저에게 귀속되는 순수익
        long netProceeds = tradeAmount - repaidLoanAmount;

        account.setBalance(account.getBalance() + netProceeds);
        userAccountRepository.save(account);

        if (position.isEmpty()) {
            leveragePositionRepository.delete(position);
            redisSyncer.remove(event.username(), event.stockCode(), leverageRatio);
        } else {
            leveragePositionRepository.save(position);
            redisSyncer.sync(position);
        }

        // 매도는 사전에 현금을 예약하지 않으므로(수량만 reserveLeverageStock으로 예약) reservedCash는 건드릴 필요 없이
        // DB balance 증가분(netProceeds)을 availableCash에 그대로 반영하면 된다.
        boolean credited = accountBalanceCommand.creditAvailableCash(event.username(), netProceeds);
        if (!credited) {
            log.error("Redis availableCash credit failed for leverage sell. username={}, stockCode={}, netProceeds={}",
                    event.username(), event.stockCode(), netProceeds);
            throw new IllegalStateException(
                    "Redis leverage sell credit failed. username=%s, stockCode=%s"
                            .formatted(event.username(), event.stockCode()));
        }

        log.info("Leverage sell applied. username={}, stockCode={}, leverageRatio={}, tradeAmount={}, " +
                        "repaidLoan={}, netProceeds={}, positionRemaining={}",
                event.username(), event.stockCode(), leverageRatio, tradeAmount, repaidLoanAmount, netProceeds,
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
        if (position.getMarginStatus() == arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus.LIQUIDATION_PENDING) {
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
