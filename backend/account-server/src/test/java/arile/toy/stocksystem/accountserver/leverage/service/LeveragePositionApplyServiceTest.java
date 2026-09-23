package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.trade.dto.TradeType;
import arile.toy.stocksystem.accountserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.accountserver.trade.service.TradeCostCalculator;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeveragePositionApplyServiceTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final LeverageRatio RATIO = LeverageRatio.X2; // 증거금 50%, 대출 50%

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private LeveragePositionRedisSyncer redisSyncer;
    @Mock private AccountMarginStatusSyncer accountMarginStatusSyncer;
    @Mock private AccountBalanceCommand accountBalanceCommand;
    @Mock private TradeCostCalculator tradeCostCalculator;

    @InjectMocks
    private LeveragePositionApplyService service;

    // ===================== fixture =====================

    private static TradeExecutedEvent event(TradeType tradeType, int orderPrice, int tradePrice, int quantity,
                                            Long reservedFeeConsumed, Long reservedMarginConsumed) {
        return new TradeExecutedEvent(1L, 10L, USERNAME, STOCK_CODE, tradeType, RATIO,
                orderPrice, tradePrice, quantity, reservedFeeConsumed, reservedMarginConsumed);
    }

    private UserAccountEntity givenAccount(long balance) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, balance);
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(account));
        return account;
    }

    private void givenPosition(LeveragePositionEntity position) {
        given(leveragePositionRepository.findByUsernameAndStockCodeAndLeverageRatioForUpdate(
                USERNAME, STOCK_CODE, RATIO)).willReturn(Optional.ofNullable(position));
    }

    /** 수량·매입금액·원가로 X2 포지션 생성 (대출금 = 매입금액의 50%) */
    private static LeveragePositionEntity position(int quantity, long purchaseAmount, long costAmount) {
        return LeveragePositionEntity.of(USERNAME, STOCK_CODE, RATIO, quantity, purchaseAmount, costAmount);
    }

    // ===================== 레버리지 매수 =====================

    @Nested
    @DisplayName("applyLeverageBuy")
    class ApplyLeverageBuy {

        // 주문가 70,000 / 체결가 69,000 / 10주 → 체결금액 690,000
        // 체결 증거금 345,000 (예약 350,000 → 환급 5,000), 대출 345,000
        // 실제 수수료 103 (예약 105 → 환급 2)
        private final TradeExecutedEvent event =
                event(TradeType.BUY, 70_000, 69_000, 10, 105L, 350_000L);

        private void givenFee() {
            given(tradeCostCalculator.calculateFee(690_000L)).willReturn(103L);
        }

        @Test
        @DisplayName("신규 포지션을 생성하고 증거금+실제 수수료만 잔고에서 차감한다")
        void withNewPosition() {
            givenFee();
            UserAccountEntity account = givenAccount(1_000_000L);
            givenPosition(null);
            given(accountBalanceCommand.settleLeverageBuy(USERNAME, 350_105L, 5_002L)).willReturn(true);

            service.applyLeverageBuy(event, RATIO);

            assertThat(account.getBalance()).isEqualTo(1_000_000L - 345_000L - 103L);
            verify(userAccountRepository).save(account);

            ArgumentCaptor<LeveragePositionEntity> captor = ArgumentCaptor.forClass(LeveragePositionEntity.class);
            verify(leveragePositionRepository).save(captor.capture());
            LeveragePositionEntity saved = captor.getValue();
            assertThat(saved.getLeverageRatio()).isEqualTo(RATIO);
            assertThat(saved.getQuantity()).isEqualTo(10);
            assertThat(saved.getAvailableQuantity()).isEqualTo(10);
            assertThat(saved.getPurchaseAmount()).isEqualTo(690_000L);
            assertThat(saved.getCostAmount()).isEqualTo(690_103L);
            assertThat(saved.getLoanAmount()).isEqualTo(345_000L);

            verify(redisSyncer).sync(saved);
            // 해제할 예약분 = 예약 증거금 + 예약 수수료, 환급액 = 증거금 차액 + 수수료 차액
            verify(accountBalanceCommand).settleLeverageBuy(USERNAME, 350_105L, 5_002L);
        }

        @Test
        @DisplayName("기존 포지션에 수량·매입금액·원가·대출금을 누적한다")
        void withExistingPosition() {
            givenFee();
            givenAccount(1_000_000L);
            LeveragePositionEntity existing = position(5, 350_000L, 350_050L); // 대출 175,000
            givenPosition(existing);
            given(accountBalanceCommand.settleLeverageBuy(USERNAME, 350_105L, 5_002L)).willReturn(true);

            service.applyLeverageBuy(event, RATIO);

            assertThat(existing.getQuantity()).isEqualTo(15);
            assertThat(existing.getAvailableQuantity()).isEqualTo(15);
            assertThat(existing.getPurchaseAmount()).isEqualTo(1_040_000L);
            assertThat(existing.getCostAmount()).isEqualTo(350_050L + 690_103L);
            assertThat(existing.getLoanAmount()).isEqualTo(175_000L + 345_000L);
            verify(leveragePositionRepository).save(existing);
            verify(redisSyncer).sync(existing);
        }

        @Test
        @DisplayName("예약 증거금·수수료가 null이면 0으로 간주해 정산한다")
        void whenReservedValuesNull_treatsAsZero() {
            TradeExecutedEvent noReserveEvent = event(TradeType.BUY, 70_000, 69_000, 10, null, null);
            givenFee();
            givenAccount(1_000_000L);
            givenPosition(null);
            given(accountBalanceCommand.settleLeverageBuy(USERNAME, 0L, -345_000L - 103L)).willReturn(true);

            service.applyLeverageBuy(noReserveEvent, RATIO);

            verify(accountBalanceCommand).settleLeverageBuy(USERNAME, 0L, -345_103L);
        }

        @Test
        @DisplayName("계좌가 없으면 IllegalArgumentException을 던진다")
        void whenAccountNotFound_throws() {
            givenFee();
            given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.applyLeverageBuy(event, RATIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Account not found");

            verifyNoInteractions(leveragePositionRepository, redisSyncer, accountBalanceCommand);
        }

        @Test
        @DisplayName("잔고가 증거금+수수료보다 적으면 불일치로 보고 예외를 던진다")
        void whenBalanceInsufficient_throws() {
            givenFee();
            UserAccountEntity account = givenAccount(345_102L);

            assertThatThrownBy(() -> service.applyLeverageBuy(event, RATIO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("balance inconsistency");

            assertThat(account.getBalance()).isEqualTo(345_102L);
            verify(userAccountRepository, never()).save(any());
            verifyNoInteractions(leveragePositionRepository, accountBalanceCommand);
        }

        @Test
        @DisplayName("Redis 예약금 정산에 실패하면 예외를 던진다")
        void whenSettleFails_throws() {
            givenFee();
            givenAccount(1_000_000L);
            givenPosition(null);
            given(accountBalanceCommand.settleLeverageBuy(USERNAME, 350_105L, 5_002L)).willReturn(false);

            assertThatThrownBy(() -> service.applyLeverageBuy(event, RATIO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Redis leverage buy settlement failed");
        }
    }

    // ===================== 레버리지 매도 =====================

    @Nested
    @DisplayName("applyLeverageSell")
    class ApplyLeverageSell {

        // 체결가 71,000 / 4주 → 매도 대금 284,000, 수수료 42 + 세금 568 = 비용 610
        private final TradeExecutedEvent event =
                event(TradeType.SELL, 70_000, 71_000, 4, null, null);

        private void givenCost() {
            given(tradeCostCalculator.calculateFee(284_000L)).willReturn(42L);
            given(tradeCostCalculator.calculateTax(284_000L)).willReturn(568L);
        }

        @Test
        @DisplayName("일부 매도 시 대출금을 비례 상환하고 순수익만 잔고에 더한 뒤 포지션을 저장·동기화한다")
        void partialSell() {
            givenCost();
            UserAccountEntity account = givenAccount(100_000L);
            LeveragePositionEntity position = position(10, 700_000L, 700_100L); // 대출 350,000
            givenPosition(position);
            // 상환 대출금 350,000 × 4/10 = 140,000 → 순수익 284,000 - 140,000 - 610 = 143,390
            given(accountBalanceCommand.creditAvailableCash(USERNAME, 143_390L)).willReturn(true);

            service.applyLeverageSell(event, RATIO);

            assertThat(account.getBalance()).isEqualTo(100_000L + 143_390L);
            verify(userAccountRepository).save(account);

            assertThat(position.getQuantity()).isEqualTo(6);
            assertThat(position.getPurchaseAmount()).isEqualTo(420_000L);
            assertThat(position.getCostAmount()).isEqualTo(420_060L);
            assertThat(position.getLoanAmount()).isEqualTo(210_000L);
            verify(leveragePositionRepository).save(position);
            verify(redisSyncer).sync(position);

            verify(leveragePositionRepository, never()).delete(any());
            verifyNoInteractions(accountMarginStatusSyncer);
            verify(accountBalanceCommand).creditAvailableCash(USERNAME, 143_390L);
        }

        @Test
        @DisplayName("전량 매도 시 대출금을 전액 상환하고 포지션을 삭제한 뒤 계좌 마진 상태를 재동기화한다")
        void fullSell() {
            givenCost();
            givenAccount(100_000L);
            LeveragePositionEntity position = position(4, 280_000L, 280_040L); // 대출 140,000
            givenPosition(position);
            given(accountBalanceCommand.creditAvailableCash(USERNAME, 143_390L)).willReturn(true);

            service.applyLeverageSell(event, RATIO);

            assertThat(position.getQuantity()).isZero();
            assertThat(position.getLoanAmount()).isZero();
            verify(leveragePositionRepository).delete(position);
            verify(redisSyncer).remove(USERNAME, STOCK_CODE, RATIO);
            verify(accountMarginStatusSyncer).resync(USERNAME);

            verify(leveragePositionRepository, never()).save(any());
            verify(redisSyncer, never()).sync(any());
            verify(accountBalanceCommand).creditAvailableCash(USERNAME, 143_390L);
        }

        @Test
        @DisplayName("계좌가 없으면 IllegalArgumentException을 던진다")
        void whenAccountNotFound_throws() {
            givenCost();
            given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.applyLeverageSell(event, RATIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Account not found");
        }

        @Test
        @DisplayName("포지션이 없으면 예외를 던진다")
        void whenPositionNotFound_throws() {
            givenCost();
            givenAccount(100_000L);
            givenPosition(null);

            assertThatThrownBy(() -> service.applyLeverageSell(event, RATIO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Leverage position not found.");

            verifyNoInteractions(accountBalanceCommand);
        }

        @Test
        @DisplayName("보유 수량이 체결 수량보다 적으면 불일치로 보고 예외를 던진다")
        void whenQuantityInsufficient_throws() {
            givenCost();
            UserAccountEntity account = givenAccount(100_000L);
            givenPosition(position(3, 210_000L, 210_030L));

            assertThatThrownBy(() -> service.applyLeverageSell(event, RATIO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("quantity inconsistency");

            assertThat(account.getBalance()).isEqualTo(100_000L);
            verify(userAccountRepository, never()).save(any());
            verifyNoInteractions(accountBalanceCommand);
        }

        @Test
        @DisplayName("Redis 가용현금 입금에 실패하면 예외를 던진다")
        void whenCreditFails_throws() {
            givenCost();
            givenAccount(100_000L);
            givenPosition(position(10, 700_000L, 700_100L));
            given(accountBalanceCommand.creditAvailableCash(USERNAME, 143_390L)).willReturn(false);

            assertThatThrownBy(() -> service.applyLeverageSell(event, RATIO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Redis leverage sell credit failed");
        }
    }

    // ===================== 매도 예약 / 환불 =====================

    @Nested
    @DisplayName("reserveLeverageStock")
    class ReserveLeverageStock {

        @Test
        @DisplayName("포지션이 없으면 false를 반환한다")
        void whenPositionNotFound_returnsFalse() {
            givenPosition(null);

            assertThat(service.reserveLeverageStock(USERNAME, STOCK_CODE, RATIO, 1)).isFalse();

            verify(leveragePositionRepository, never()).save(any());
            verifyNoInteractions(redisSyncer);
        }

        @Test
        @DisplayName("매도 가능 수량이 부족하면 false를 반환한다")
        void whenAvailableQuantityInsufficient_returnsFalse() {
            LeveragePositionEntity position = position(10, 700_000L, 700_100L);
            position.setAvailableQuantity(3);
            givenPosition(position);

            assertThat(service.reserveLeverageStock(USERNAME, STOCK_CODE, RATIO, 4)).isFalse();

            assertThat(position.getAvailableQuantity()).isEqualTo(3);
            verifyNoInteractions(redisSyncer);
        }

        @Test
        @DisplayName("청산 확정(LIQUIDATION_PENDING) 포지션은 매도 예약할 수 없다")
        void whenLiquidationPending_returnsFalse() {
            LeveragePositionEntity position = position(10, 700_000L, 700_100L);
            position.changeMarginStatus(MarginStatus.LIQUIDATION_PENDING, LocalDate.now());
            givenPosition(position);

            assertThat(service.reserveLeverageStock(USERNAME, STOCK_CODE, RATIO, 4)).isFalse();

            assertThat(position.getAvailableQuantity()).isEqualTo(10);
            verifyNoInteractions(redisSyncer);
        }

        @Test
        @DisplayName("매도 가능 수량을 차감하고 저장·동기화한 뒤 true를 반환한다")
        void success() {
            LeveragePositionEntity position = position(10, 700_000L, 700_100L);
            givenPosition(position);

            assertThat(service.reserveLeverageStock(USERNAME, STOCK_CODE, RATIO, 4)).isTrue();

            assertThat(position.getAvailableQuantity()).isEqualTo(6);
            assertThat(position.getQuantity()).isEqualTo(10);
            verify(leveragePositionRepository).save(position);
            verify(redisSyncer).sync(position);
        }

        @Test
        @DisplayName("마진콜 상태여도 청산 확정 전이면 매도 예약할 수 있다 (가용 수량 전량)")
        void whenMarginCall_allowsFullAvailableQuantity() {
            LeveragePositionEntity position = position(10, 700_000L, 700_100L);
            position.changeMarginStatus(MarginStatus.MARGIN_CALL, LocalDate.now());
            givenPosition(position);

            assertThat(service.reserveLeverageStock(USERNAME, STOCK_CODE, RATIO, 10)).isTrue();

            assertThat(position.getAvailableQuantity()).isZero();
        }
    }

    @Nested
    @DisplayName("refundReservedLeverageStock")
    class RefundReservedLeverageStock {

        @Test
        @DisplayName("포지션이 없으면 false를 반환한다")
        void whenPositionNotFound_returnsFalse() {
            givenPosition(null);

            assertThat(service.refundReservedLeverageStock(USERNAME, STOCK_CODE, RATIO, 4)).isFalse();

            verify(leveragePositionRepository, never()).save(any());
            verifyNoInteractions(redisSyncer);
        }

        @Test
        @DisplayName("예약된 매도 가능 수량을 되돌리고 저장·동기화한 뒤 true를 반환한다")
        void success() {
            LeveragePositionEntity position = position(10, 700_000L, 700_100L);
            position.setAvailableQuantity(6);
            givenPosition(position);

            assertThat(service.refundReservedLeverageStock(USERNAME, STOCK_CODE, RATIO, 4)).isTrue();

            assertThat(position.getAvailableQuantity()).isEqualTo(10);
            verify(leveragePositionRepository).save(position);
            verify(redisSyncer).sync(position);
        }
    }
}
