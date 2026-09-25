package arile.toy.stocksystem.accountserver.trade.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.service.LeveragePositionApplyService;
import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.publisher.RankUpdatedPublisher;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.trade.TradeCommand;
import arile.toy.stocksystem.accountserver.trade.dto.TradeType;
import arile.toy.stocksystem.accountserver.trade.entity.AppliedTradeEntity;
import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.trade.repository.AppliedTradeRepository;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import arile.toy.stocksystem.accountserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.accountserver.userstock.repository.UserStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TradeExecutionApplyServiceTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserStockRepository userStockRepository;
    @Mock private TradeCommand tradeCommand;
    @Mock private AccountUpdateEventPublisher accountUpdateEventPublisher;
    @Mock private UserRankRepository userRankRepository;
    @Mock private LeveragePositionApplyService leveragePositionApplyService;
    @Mock private TradeCostCalculator tradeCostCalculator;
    @Mock private AccountBalanceCommand accountBalanceCommand;
    @Mock private RankUpdatedPublisher rankUpdatedPublisher;
    @Mock private AppliedTradeRepository appliedTradeRepository;

    @InjectMocks
    private TradeExecutionApplyService service;

    // ===================== fixture =====================

    private static TradeExecutedEvent event(TradeType tradeType, LeverageRatio leverageRatio,
                                            int orderPrice, int tradePrice, int quantity,
                                            Long reservedFeeConsumed) {
        return new TradeExecutedEvent(1L, 10L, USERNAME, STOCK_CODE, tradeType, leverageRatio,
                orderPrice, tradePrice, quantity, reservedFeeConsumed, null);
    }

    private UserAccountEntity givenAccount(long balance) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, balance);
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(account));
        return account;
    }

    private UserRankEntity givenRank(boolean entered) {
        UserRankEntity rank = UserRankEntity.of(USERNAME, 1_000_000L);
        if (entered) {
            rank.setEntered(true);
            rank.setCurrentLevel(RankLevel.SILVER_3);
            rank.setHighestTierReached(RankLevel.SILVER_3);
        }
        given(userRankRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(rank));
        return rank;
    }

    private void givenStock(UserStockEntity stock) {
        given(userStockRepository.findByUsernameAndStockCode(USERNAME, STOCK_CODE))
                .willReturn(Optional.ofNullable(stock));
    }

    // ===================== 중복 체결 방지 =====================

    @Nested
    @DisplayName("apply: 중복 체결 방지")
    class Idempotency {

        private final TradeExecutedEvent event =
                event(TradeType.BUY, LeverageRatio.X2, 70_000, 69_000, 10, 105L);

        @Test
        @DisplayName("이미 반영한 체결(종목코드 + 체결 ID)이면 기록·정산·랭크·이벤트를 모두 건너뛴다")
        void alreadyApplied_skipsEverything() {
            given(appliedTradeRepository.existsByStockCodeAndTradeId(STOCK_CODE, 1L)).willReturn(true);

            service.apply(event);

            verify(appliedTradeRepository, never()).saveAndFlush(any());
            verifyNoInteractions(userAccountRepository, userStockRepository, tradeCommand,
                    leveragePositionApplyService, userRankRepository, accountUpdateEventPublisher,
                    accountBalanceCommand, rankUpdatedPublisher);
        }

        @Test
        @DisplayName("처음 반영하는 체결은 정산보다 먼저 반영 기록을 저장한다")
        void newTrade_recordsBeforeSettlement() {
            givenRank(true);

            service.apply(event);

            InOrder inOrder = inOrder(appliedTradeRepository, leveragePositionApplyService);
            inOrder.verify(appliedTradeRepository).saveAndFlush(argThat((AppliedTradeEntity e) ->
                    e.getStockCode().equals(STOCK_CODE) && e.getTradeId().equals(1L)));
            inOrder.verify(leveragePositionApplyService).applyLeverageBuy(event, LeverageRatio.X2);
        }

        @Test
        @DisplayName("동시 처리로 반영 기록 저장이 유니크 제약에 걸리면 정산 전에 예외를 던진다")
        void concurrentDuplicate_throwsBeforeSettlement() {
            given(appliedTradeRepository.saveAndFlush(any(AppliedTradeEntity.class)))
                    .willThrow(new DataIntegrityViolationException("uk_applied_trade"));

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(DataIntegrityViolationException.class);

            verifyNoInteractions(userAccountRepository, userStockRepository, tradeCommand,
                    leveragePositionApplyService, userRankRepository, accountUpdateEventPublisher);
        }

        @Test
        @DisplayName("체결 ID가 없으면 예외를 던지고 아무것도 반영하지 않는다")
        void nullTradeId_throws() {
            TradeExecutedEvent noIdEvent = new TradeExecutedEvent(null, 10L, USERNAME, STOCK_CODE,
                    TradeType.BUY, LeverageRatio.SPOT, 70_000, 70_000, 1, 11L, null);

            assertThatThrownBy(() -> service.apply(noIdEvent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("tradeId is required to apply trade.");

            verifyNoInteractions(appliedTradeRepository, userAccountRepository, tradeCommand,
                    leveragePositionApplyService, userRankRepository);
        }
    }

    // ===================== 라우팅 =====================

    @Nested
    @DisplayName("apply: 레버리지 배율에 따른 라우팅")
    class Routing {

        @Test
        @DisplayName("레버리지 매수는 LeveragePositionApplyService로 위임하고 현물 로직을 타지 않는다")
        void leverageBuy_delegates() {
            TradeExecutedEvent event = event(TradeType.BUY, LeverageRatio.X2, 70_000, 69_000, 10, 105L);
            givenRank(true);

            service.apply(event);

            verify(leveragePositionApplyService).applyLeverageBuy(event, LeverageRatio.X2);
            verifyNoInteractions(tradeCommand, userStockRepository);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("레버리지 매도는 LeveragePositionApplyService로 위임하고 현물 로직을 타지 않는다")
        void leverageSell_delegates() {
            TradeExecutedEvent event = event(TradeType.SELL, LeverageRatio.X1_5, 70_000, 71_000, 4, null);
            givenRank(true);

            service.apply(event);

            verify(leveragePositionApplyService).applyLeverageSell(event, LeverageRatio.X1_5);
            verifyNoInteractions(tradeCommand, userStockRepository);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("leverageRatio가 null이면 현물(SPOT)로 처리한다")
        void nullLeverageRatio_treatedAsSpot() {
            TradeExecutedEvent event = event(TradeType.BUY, null, 70_000, 70_000, 1, 11L);
            given(tradeCostCalculator.calculateFee(70_000L)).willReturn(10L);
            givenAccount(1_000_000L);
            givenStock(null);
            given(tradeCommand.applyBuyTrade(anyString(), anyString(), anyInt(),
                    anyLong(), anyLong(), anyLong(), anyLong())).willReturn(true);
            givenRank(true);

            service.apply(event);

            verifyNoInteractions(leveragePositionApplyService);
            verify(tradeCommand).applyBuyTrade(anyString(), anyString(), anyInt(),
                    anyLong(), anyLong(), anyLong(), anyLong());
        }
    }

    // ===================== 현물 매수 =====================

    @Nested
    @DisplayName("apply: 현물 매수")
    class SpotBuy {

        // 주문가 70,000 / 체결가 69,000 / 10주 → 체결금액 690,000, 주문금액 700,000, 가격차 환급 10,000
        // 예약 수수료 105, 실제 수수료 103 → 수수료 환급 2
        private final TradeExecutedEvent event =
                event(TradeType.BUY, LeverageRatio.SPOT, 70_000, 69_000, 10, 105L);

        private void givenFee() {
            given(tradeCostCalculator.calculateFee(690_000L)).willReturn(103L);
        }

        private void givenRedisResult(boolean result) {
            given(tradeCommand.applyBuyTrade(anyString(), anyString(), anyInt(),
                    anyLong(), anyLong(), anyLong(), anyLong())).willReturn(result);
        }

        @Test
        @DisplayName("기존 보유 종목에 수량·매입금액·원가를 누적하고 체결금액+실제 수수료를 잔고에서 차감한다")
        void withExistingStock() {
            givenFee();
            UserAccountEntity account = givenAccount(1_000_000L);
            UserStockEntity stock = UserStockEntity.of(USERNAME, STOCK_CODE, 350_000L, 350_050L, 5);
            givenStock(stock);
            givenRedisResult(true);
            givenRank(true);

            service.apply(event);

            assertThat(account.getBalance()).isEqualTo(1_000_000L - 690_000L - 103L);
            verify(userAccountRepository).save(account);

            assertThat(stock.getQuantity()).isEqualTo(15);
            assertThat(stock.getAmount()).isEqualTo(1_040_000L);
            assertThat(stock.getCostAmount()).isEqualTo(350_050L + 690_000L + 103L);
            verify(userStockRepository).save(stock);

            // 해제할 예약금 = 주문금액 + 예약 수수료, 환급액 = 가격차 + 수수료 차액
            verify(tradeCommand).applyBuyTrade(USERNAME, STOCK_CODE, 15,
                    1_040_000L, 1_040_153L, 700_105L, 10_002L);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("보유하지 않은 종목이면 새로 생성한다")
        void withNewStock() {
            givenFee();
            givenAccount(1_000_000L);
            givenStock(null);
            givenRedisResult(true);
            givenRank(true);

            service.apply(event);

            ArgumentCaptor<UserStockEntity> captor = ArgumentCaptor.forClass(UserStockEntity.class);
            verify(userStockRepository).save(captor.capture());
            UserStockEntity saved = captor.getValue();
            assertThat(saved.getUsername()).isEqualTo(USERNAME);
            assertThat(saved.getStockCode()).isEqualTo(STOCK_CODE);
            assertThat(saved.getQuantity()).isEqualTo(10);
            assertThat(saved.getAmount()).isEqualTo(690_000L);
            assertThat(saved.getCostAmount()).isEqualTo(690_103L);

            verify(tradeCommand).applyBuyTrade(USERNAME, STOCK_CODE, 10,
                    690_000L, 690_103L, 700_105L, 10_002L);
        }

        @Test
        @DisplayName("예약 수수료가 null이면 0으로 간주해 실제 수수료만큼 환급액에서 뺀다")
        void whenReservedFeeNull_treatsAsZero() {
            TradeExecutedEvent noFeeEvent =
                    event(TradeType.BUY, LeverageRatio.SPOT, 70_000, 69_000, 10, null);
            givenFee();
            givenAccount(1_000_000L);
            givenStock(null);
            givenRedisResult(true);
            givenRank(true);

            service.apply(noFeeEvent);

            verify(tradeCommand).applyBuyTrade(USERNAME, STOCK_CODE, 10,
                    690_000L, 690_103L, 700_000L, 10_000L - 103L);
        }

        @Test
        @DisplayName("잔고가 체결금액과 실제 수수료의 합과 정확히 같으면 반영한다")
        void whenBalanceExactlyEnough_applies() {
            givenFee();
            UserAccountEntity account = givenAccount(690_103L);
            givenStock(null);
            givenRedisResult(true);
            givenRank(true);

            service.apply(event);

            assertThat(account.getBalance()).isZero();
        }

        @Test
        @DisplayName("계좌가 없으면 IllegalArgumentException을 던진다")
        void whenAccountNotFound_throws() {
            givenFee();
            given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Account not found");

            verifyNoInteractions(userStockRepository, tradeCommand, accountUpdateEventPublisher);
        }

        @Test
        @DisplayName("DB 잔고가 체결금액+수수료보다 적으면 불일치로 보고 예외를 던진다")
        void whenBalanceInsufficient_throws() {
            givenFee();
            UserAccountEntity account = givenAccount(690_102L);

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("balance inconsistency");

            assertThat(account.getBalance()).isEqualTo(690_102L);
            verify(userAccountRepository, never()).save(any());
            verifyNoInteractions(userStockRepository, tradeCommand);
        }

        @Test
        @DisplayName("Redis 반영에 실패하면 예외를 던지고 랭크·이벤트를 처리하지 않는다")
        void whenRedisFails_throws() {
            givenFee();
            givenAccount(1_000_000L);
            givenStock(null);
            givenRedisResult(false);

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Redis buy trade apply failed");

            verifyNoInteractions(userRankRepository, accountUpdateEventPublisher);
        }
    }

    // ===================== 현물 매도 =====================

    @Nested
    @DisplayName("apply: 현물 매도")
    class SpotSell {

        // 주문가 70,000 / 체결가 71,000 / 4주 → 체결금액 284,000, 주문금액 280,000, 가격차 4,000
        // 수수료 42 + 세금 511 = 비용 553
        private final TradeExecutedEvent event =
                event(TradeType.SELL, LeverageRatio.SPOT, 70_000, 71_000, 4, null);

        private void givenCost(long fee, long tax) {
            given(tradeCostCalculator.calculateFee(284_000L)).willReturn(fee);
            given(tradeCostCalculator.calculateTax(284_000L)).willReturn(tax);
        }

        private void givenRedisResult(boolean result) {
            given(tradeCommand.applySellTrade(anyString(), anyString(), anyInt(),
                    anyLong(), anyLong(), anyLong(), anyLong())).willReturn(result);
        }

        @Test
        @DisplayName("일부 매도 시 수량 비례로 매입금액·원가를 줄이고, 비용을 뺀 대금을 잔고에 더한 뒤 비용을 Redis에서 차감한다")
        void partialSell() {
            givenCost(42L, 511L);
            UserAccountEntity account = givenAccount(100_000L);
            UserStockEntity stock = UserStockEntity.of(USERNAME, STOCK_CODE, 700_000L, 700_100L, 10);
            givenStock(stock);
            givenRedisResult(true);
            given(accountBalanceCommand.debitAvailableCash(USERNAME, 553L)).willReturn(true);
            givenRank(true);

            service.apply(event);

            assertThat(account.getBalance()).isEqualTo(100_000L + 284_000L - 553L);

            // 700,000 × 4/10 = 280,000 매도 → 잔여 420,000 / 700,100 × 4/10 = 280,040 → 잔여 420,060
            assertThat(stock.getQuantity()).isEqualTo(6);
            assertThat(stock.getAmount()).isEqualTo(420_000L);
            assertThat(stock.getCostAmount()).isEqualTo(420_060L);
            verify(userStockRepository).save(stock);
            verify(userStockRepository, never()).delete(any());

            verify(tradeCommand).applySellTrade(USERNAME, STOCK_CODE, 6,
                    420_000L, 420_060L, 280_000L, 4_000L);
            verify(accountBalanceCommand).debitAvailableCash(USERNAME, 553L);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("전량 매도 시 보유 종목을 삭제한다")
        void fullSell_deletesStock() {
            givenCost(42L, 511L);
            givenAccount(100_000L);
            UserStockEntity stock = UserStockEntity.of(USERNAME, STOCK_CODE, 280_000L, 280_040L, 4);
            givenStock(stock);
            givenRedisResult(true);
            given(accountBalanceCommand.debitAvailableCash(USERNAME, 553L)).willReturn(true);
            givenRank(true);

            service.apply(event);

            verify(userStockRepository).delete(stock);
            verify(userStockRepository, never()).save(any());
            verify(tradeCommand).applySellTrade(USERNAME, STOCK_CODE, 0,
                    0L, 0L, 280_000L, 4_000L);
        }

        @Test
        @DisplayName("수수료와 세금이 모두 0이면 Redis 비용 차감을 생략한다")
        void whenNoCost_skipsDebit() {
            givenCost(0L, 0L);
            givenAccount(100_000L);
            givenStock(UserStockEntity.of(USERNAME, STOCK_CODE, 700_000L, 700_100L, 10));
            givenRedisResult(true);
            givenRank(true);

            service.apply(event);

            verifyNoInteractions(accountBalanceCommand);
        }

        @Test
        @DisplayName("계좌가 없으면 IllegalArgumentException을 던진다")
        void whenAccountNotFound_throws() {
            givenCost(42L, 511L);
            given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Account not found");
        }

        @Test
        @DisplayName("보유 종목이 없으면 예외를 던진다")
        void whenStockNotOwned_throws() {
            givenCost(42L, 511L);
            givenAccount(100_000L);
            givenStock(null);

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No stocks owned.");

            verifyNoInteractions(tradeCommand);
        }

        @Test
        @DisplayName("보유 수량이 체결 수량보다 적으면 불일치로 보고 예외를 던진다")
        void whenQuantityInsufficient_throws() {
            givenCost(42L, 511L);
            givenAccount(100_000L);
            givenStock(UserStockEntity.of(USERNAME, STOCK_CODE, 210_000L, 210_030L, 3));

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("stock quantity inconsistency");

            verifyNoInteractions(tradeCommand);
        }

        @Test
        @DisplayName("Redis 반영에 실패하면 예외를 던지고 비용을 차감하지 않는다")
        void whenRedisFails_throws() {
            givenCost(42L, 511L);
            givenAccount(100_000L);
            givenStock(UserStockEntity.of(USERNAME, STOCK_CODE, 700_000L, 700_100L, 10));
            givenRedisResult(false);

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(accountBalanceCommand, userRankRepository, accountUpdateEventPublisher);
        }

        @Test
        @DisplayName("Redis 비용 차감에 실패하면 예외를 던지고 랭크·이벤트를 처리하지 않는다")
        void whenDebitFails_throws() {
            givenCost(42L, 511L);
            givenAccount(100_000L);
            givenStock(UserStockEntity.of(USERNAME, STOCK_CODE, 700_000L, 700_100L, 10));
            givenRedisResult(true);
            given(accountBalanceCommand.debitAvailableCash(USERNAME, 553L)).willReturn(false);

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fee/tax debit failed");

            verifyNoInteractions(userRankRepository, accountUpdateEventPublisher);
        }
    }

    // ===================== 랭크 =====================

    @Nested
    @DisplayName("apply: 거래 시 랭크 갱신")
    class RankUpdate {

        private final TradeExecutedEvent event =
                event(TradeType.BUY, LeverageRatio.X2, 70_000, 69_000, 10, 105L);

        @Test
        @DisplayName("첫 거래면 BRONZE_5로 배치하고 랭크 갱신 이벤트를 발행한다")
        void firstTrade_entersBronze5AndPublishes() {
            UserRankEntity rank = givenRank(false);

            service.apply(event);

            assertThat(rank.getEntered()).isTrue();
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.BRONZE_5);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.BRONZE_5);
            assertThat(rank.getDailyTradeAmount()).isEqualTo(690_000L);
            verify(userRankRepository).save(rank);
            verify(rankUpdatedPublisher).publish();
        }

        @Test
        @DisplayName("이미 참여한 사용자는 등급을 유지하고 당일 거래대금만 누적한다")
        void existingParticipant_accumulatesOnly() {
            UserRankEntity rank = givenRank(true);

            service.apply(event);

            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.SILVER_3);
            assertThat(rank.getDailyTradeAmount()).isEqualTo(690_000L);
            verify(userRankRepository).save(rank);
            verifyNoInteractions(rankUpdatedPublisher);
        }

        @Test
        @DisplayName("랭크 정보가 없으면 예외를 던지고 계좌 이벤트를 발행하지 않는다")
        void whenRankNotFound_throws() {
            given(userRankRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.apply(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("UserRank not found: " + USERNAME);

            verifyNoInteractions(accountUpdateEventPublisher);
        }
    }
}
