package arile.toy.stocksystem.accountserver.trade.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.service.LeveragePositionApplyService;
import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.trade.TradeCommand;
import arile.toy.stocksystem.accountserver.trade.dto.TradeType;
import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import arile.toy.stocksystem.accountserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.accountserver.userstock.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionApplyService {

    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    private final TradeCommand tradeCommand;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;
    private final UserRankRepository userRankRepository;
    private final LeveragePositionApplyService leveragePositionApplyService;
    private final TradeCostCalculator tradeCostCalculator;

    @Transactional
    public void apply(TradeExecutedEvent event) {

        LeverageRatio leverageRatio = event.leverageRatio() == null ? LeverageRatio.SPOT : event.leverageRatio();

        if (leverageRatio.isSpot()) {
            if (event.tradeType() == TradeType.BUY) {
                applyBuy(event);
            } else {
                applySell(event);
            }
        } else {
            if (event.tradeType() == TradeType.BUY) {
                leveragePositionApplyService.applyLeverageBuy(event, leverageRatio);
            } else {
                leveragePositionApplyService.applyLeverageSell(event, leverageRatio);
            }
        }

        long tradeAmount = (long) event.tradePrice() * event.tradeQuantity();
        updateRankOnTrade(event.username(), tradeAmount);
        accountUpdateEventPublisher.publish(event.username());
    }

    private void applyBuy(TradeExecutedEvent event) {

        int executable = event.tradeQuantity();
        long tradeAmount = (long) event.tradePrice() * executable;

        // 예약(reserve) 당시 금액 기준.
        // reserveCash 시 orderPrice * orderQuantity + 수수료(주문가 기준)로 예약했으므로
        // 체결 시 실제 정산은 orderAmount(예약금) 기준으로 하고, 체결가와의 차액을 환급
        long orderAmount = (long) event.orderPrice() * executable;
        long differenceAmount = (long) (event.orderPrice() - event.tradePrice()) * executable;

        // 예약 당시 수수료(주문가 기준) vs 실제 수수료(체결가 기준): 매수 지정가는
        // 체결가가 항상 주문가 이하이므로 feeReserved >= feeActual, 차액은 항상 0 이상(환급)
        long feeReserved = tradeCostCalculator.calculateFee(orderAmount);
        long feeActual = tradeCostCalculator.calculateFee(tradeAmount);
        long feeRefund = feeReserved - feeActual;

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(event.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // balance에서 이미 예약 해제분(tradeAmount + feeActual)만 확정 차감
        // (feeReserved 전체가 아니라 실제 수수료만 최종 비용으로 남고, 나머지는 환급되므로 DB에는 애초에 반영할 필요 없음 —
        //  주문 시점엔 DB balance를 안 건드리고 reservedCash(Redis)만 예약했기 때문)
        if (account.getBalance() < tradeAmount + feeActual) {
            throw new IllegalStateException(
                    "DB/Redis balance inconsistency detected during trade apply."
            );
        }

        account.setBalance(account.getBalance() - tradeAmount - feeActual); // 실제 수수료만 확정 차감
        userAccountRepository.save(account);

        UserStockEntity userStock = userStockRepository
                .findByUsernameAndStockCode(event.username(), event.stockCode())
                .orElseGet(() -> UserStockEntity.of(event.username(), event.stockCode(), 0L, 0));

        int prevQuantity = userStock.getQuantity();
        userStock.setQuantity(prevQuantity + executable);

        long prevAmount = userStock.getAmount();
        userStock.setAmount(prevAmount + tradeAmount);

        userStockRepository.save(userStock);

        long totalAmount = prevAmount + tradeAmount;
        int totalQuantity = prevQuantity + executable;

        // 해제할 예약금에 feeReserved를 더하고, 환급할 차액에도 feeRefund를 더함
        boolean redisOk = tradeCommand.applyBuyTrade(
                event.username(), event.stockCode(), totalQuantity, totalAmount,
                orderAmount + feeReserved, differenceAmount + feeRefund
        );

        if (!redisOk) {
            log.error("Redis buy trade apply failed. username={}, stockCode={}",
                    event.username(), event.stockCode());
            throw new IllegalStateException(
                    "Redis buy trade apply failed. username=%s, stockCode=%s"
                            .formatted(event.username(), event.stockCode()));
        }
    }

    private void applySell(TradeExecutedEvent event) {

        int executable = event.tradeQuantity();
        long tradeAmount = (long) event.tradePrice() * executable;
        long orderAmount = (long) event.orderPrice() * executable;
        long differenceAmount = (long) (event.tradePrice() - event.orderPrice()) * executable;

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(event.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setBalance(account.getBalance() + tradeAmount);
        userAccountRepository.save(account);

        UserStockEntity userStock = userStockRepository
                .findByUsernameAndStockCode(event.username(), event.stockCode())
                .orElseThrow(() -> new RuntimeException("No stocks owned."));

        if (userStock.getQuantity() < executable) {
            throw new IllegalStateException(
                    "DB/Redis stock quantity inconsistency detected during trade apply."
            );
        }

        int prevQuantity = userStock.getQuantity();
        long prevAmount = userStock.getAmount();
        int totalQuantity = prevQuantity - executable;

        long soldAmount = prevAmount * executable / prevQuantity;
        long remainingAmount = prevAmount - soldAmount;

        userStock.setQuantity(totalQuantity);
        if (totalQuantity == 0) {
            userStockRepository.delete(userStock);
        } else {
            userStock.setAmount(remainingAmount);
            userStockRepository.save(userStock);
        }

        boolean redisOk = tradeCommand.applySellTrade(
                event.username(), event.stockCode(), totalQuantity, remainingAmount,
                orderAmount, differenceAmount
        );

        if (!redisOk) {
            log.error("Redis sell trade apply failed. username={}, stockCode={}",
                    event.username(), event.stockCode());
            throw new IllegalStateException(
                    "Redis buy trade apply failed. username=%s, stockCode=%s"
                            .formatted(event.username(), event.stockCode()));
        }
    }

    private void updateRankOnTrade(String username, long tradeAmount) {
        UserRankEntity rank = userRankRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalStateException("UserRank not found: " + username));

        if (!rank.getEntered()) {
            rank.setEntered(true);
            rank.setCurrentLevel(RankLevel.BRONZE_5);
            rank.setHighestTierReached(RankLevel.BRONZE_5);
        }

        rank.addDailyTradeAmount(tradeAmount);
        userRankRepository.save(rank);
    }
}
