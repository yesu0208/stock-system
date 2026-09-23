package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeverageLiquidationEntity;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.event.LiquidationExecutedEvent;
import arile.toy.stocksystem.accountserver.leverage.event.publisher.LiquidationEventPublisher;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageLiquidationRepository;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.stockprice.dto.StockSummaryTickMessage;
import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class LeverageLiquidationExecutorTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final LeverageRatio RATIO = LeverageRatio.X2;
    private static final long LOAN = 350_000L;

    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private LeverageLiquidationRepository leverageLiquidationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private StockSummaryRedisRepository stockSummaryRedisRepository;
    @Mock private UserAccountRedisRepository userAccountRedisRepository;
    @Mock private LeveragePositionRedisSyncer redisSyncer;
    @Mock private AccountMarginStatusSyncer accountMarginStatusSyncer;
    @Mock private LiquidationEventPublisher liquidationEventPublisher;
    @Mock private AccountBalanceCommand accountBalanceCommand;

    @InjectMocks
    private LeverageLiquidationExecutor executor;

    // ===================== fixture =====================

    /** X2, 10주, 매입 700,000 → 대출 350,000 */
    private static LeveragePositionEntity position(long id, MarginStatus status) {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, STOCK_CODE, RATIO, 10, 700_000L, 700_100L);
        position.changeMarginStatus(status, LocalDate.now().minusDays(1));
        ReflectionTestUtils.setField(position, "leveragePositionId", id);
        return position;
    }

    private LeveragePositionEntity givenPendingPosition() {
        LeveragePositionEntity position = position(1L, MarginStatus.LIQUIDATION_PENDING);
        given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(position));
        return position;
    }

    private void givenPrice(Integer price) {
        given(stockSummaryRedisRepository.findByStockCode(STOCK_CODE))
                .willReturn(new StockSummaryTickMessage(STOCK_CODE, price, 0));
    }

    private UserAccountEntity givenAccount(long balance, AccountStatus status) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, balance);
        if (status != AccountStatus.NORMAL) {
            account.changeAccountStatus(status, LocalDate.now().minusDays(1));
        }
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(account));
        return account;
    }

    private void verifyLiquidated(LeveragePositionEntity position, long settlementPrice,
                                  long netAfterRepay, long shortfall) {
        long proceeds = 10L * settlementPrice;
        verify(leverageLiquidationRepository).save(LeverageLiquidationEntity.of(
                USERNAME, STOCK_CODE, RATIO, 10, settlementPrice, proceeds, LOAN, shortfall));
        verify(leveragePositionRepository).delete(position);
        verify(redisSyncer).remove(USERNAME, STOCK_CODE, RATIO);
        verify(accountMarginStatusSyncer).resync(USERNAME);
        verify(accountBalanceCommand).creditAvailableCash(USERNAME, netAfterRepay);
        verify(liquidationEventPublisher).publish(
                LiquidationExecutedEvent.of(USERNAME, STOCK_CODE, RATIO, 10, settlementPrice, shortfall));
    }

    // ===================== 청산 성공 =====================

    @Test
    @DisplayName("대금이 대출금보다 크면 잔액을 입금하고 부족분 없이 청산한다")
    void withoutShortfall() {
        LeveragePositionEntity position = givenPendingPosition();
        givenPrice(40_000);
        UserAccountEntity account = givenAccount(100_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.creditAvailableCash(USERNAME, 50_000L)).willReturn(true);

        boolean hadShortfall = executor.liquidateOnePosition(1L);

        assertThat(hadShortfall).isFalse();
        assertThat(account.getBalance()).isEqualTo(150_000L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        verify(userAccountRepository).save(account);
        verifyNoInteractions(userAccountRedisRepository);
        verifyLiquidated(position, 40_000L, 50_000L, 0L);
    }

    @Test
    @DisplayName("부족분이 생겨도 계좌 전체 잔고가 0 이상이면 NEGATIVE로 전환하지 않는다")
    void withShortfall_balanceStillPositive() {
        LeveragePositionEntity position = givenPendingPosition();
        givenPrice(30_000);
        UserAccountEntity account = givenAccount(100_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.creditAvailableCash(USERNAME, -50_000L)).willReturn(true);

        boolean hadShortfall = executor.liquidateOnePosition(1L);

        assertThat(hadShortfall).isTrue();
        assertThat(account.getBalance()).isEqualTo(50_000L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        verifyNoInteractions(userAccountRedisRepository);
        verifyLiquidated(position, 30_000L, -50_000L, 50_000L);
    }

    @Test
    @DisplayName("청산 후 계좌 잔고가 마이너스가 되면 NORMAL 계좌를 NEGATIVE로 전환하고 Redis에 반영한다")
    void withShortfall_balanceNegative_convertsToNegative() {
        LeveragePositionEntity position = givenPendingPosition();
        givenPrice(30_000);
        UserAccountEntity account = givenAccount(30_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.creditAvailableCash(USERNAME, -50_000L)).willReturn(true);

        boolean hadShortfall = executor.liquidateOnePosition(1L);

        assertThat(hadShortfall).isTrue();
        assertThat(account.getBalance()).isEqualTo(-20_000L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NEGATIVE);
        verify(userAccountRedisRepository).saveAccountStatus(USERNAME, "NEGATIVE");
        verify(userAccountRepository).save(account);
        verifyLiquidated(position, 30_000L, -50_000L, 50_000L);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AccountStatus.class, names = {"NEGATIVE", "SUSPENDED"})
    @DisplayName("이미 NORMAL이 아닌 계좌는 잔고가 마이너스여도 상태를 바꾸지 않는다")
    void whenAccountNotNormal_keepsStatus(AccountStatus status) {
        givenPendingPosition();
        givenPrice(30_000);
        UserAccountEntity account = givenAccount(-10_000L, status);
        given(accountBalanceCommand.creditAvailableCash(USERNAME, -50_000L)).willReturn(true);

        executor.liquidateOnePosition(1L);

        assertThat(account.getBalance()).isEqualTo(-60_000L);
        assertThat(account.getAccountStatus()).isEqualTo(status);
        verifyNoInteractions(userAccountRedisRepository);
    }

    // ===================== 청산 생략 / 실패 =====================

    @Test
    @DisplayName("포지션이 없으면 예외를 던진다")
    void whenPositionNotFound_throws() {
        given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> executor.liquidateOnePosition(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Leverage position not found. id=1");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = MarginStatus.class, names = {"NORMAL", "MARGIN_CALL"})
    @DisplayName("그 사이 상태가 바뀌어 LIQUIDATION_PENDING이 아니면 청산하지 않는다")
    void whenStatusChanged_skips(MarginStatus status) {
        given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(position(1L, status)));

        assertThat(executor.liquidateOnePosition(1L)).isFalse();

        verifyNoInteractions(stockSummaryRedisRepository, userAccountRepository, accountBalanceCommand);
        verify(leveragePositionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("주가 요약이 없으면 다음 배치로 미룬다")
    void whenSummaryNotFound_defers() {
        givenPendingPosition();
        given(stockSummaryRedisRepository.findByStockCode(STOCK_CODE)).willReturn(null);

        assertThat(executor.liquidateOnePosition(1L)).isFalse();

        verifyNoInteractions(userAccountRepository, leverageLiquidationRepository, accountBalanceCommand);
    }

    @Test
    @DisplayName("현재가가 null이면 다음 배치로 미룬다")
    void whenCurPriceNull_defers() {
        givenPendingPosition();
        givenPrice(null);

        assertThat(executor.liquidateOnePosition(1L)).isFalse();

        verifyNoInteractions(userAccountRepository, leverageLiquidationRepository, accountBalanceCommand);
    }

    @Test
    @DisplayName("계좌가 없으면 IllegalArgumentException을 던진다")
    void whenAccountNotFound_throws() {
        givenPendingPosition();
        givenPrice(40_000);
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> executor.liquidateOnePosition(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account not found. username=" + USERNAME);

        verifyNoInteractions(leverageLiquidationRepository, accountBalanceCommand);
    }

    @Test
    @DisplayName("Redis 가용현금 반영에 실패하면 예외를 던지고 청산 이벤트를 발행하지 않는다")
    void whenCreditFails_throws() {
        givenPendingPosition();
        givenPrice(40_000);
        givenAccount(100_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.creditAvailableCash(USERNAME, 50_000L)).willReturn(false);

        assertThatThrownBy(() -> executor.liquidateOnePosition(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis liquidation credit failed");

        verifyNoInteractions(liquidationEventPublisher);
    }
}
