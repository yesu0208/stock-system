package arile.toy.stocksystem.accountserver.useraccount.service;

import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.useraccount.dto.UserAccountMessage;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    private static final String USERNAME = "user1";
    private static final long INITIAL_BALANCE = 1_000_000_000L;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAccountRedisRepository userAccountRedisRepository;

    @Mock
    private UserRankRepository userRankRepository;

    @Mock
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private UserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userAccountService, "initialBalance", INITIAL_BALANCE);
    }

    @Nested
    @DisplayName("createAccountIfAbsent")
    class CreateAccountIfAbsent {

        @Test
        @DisplayName("계좌가 이미 있으면 아무것도 생성하지 않는다")
        void whenAccountExists_doesNothing() {
            given(userAccountRepository.existsByUsername(USERNAME)).willReturn(true);

            userAccountService.createAccountIfAbsent(USERNAME);

            verify(userAccountRepository, never()).save(any());
            verifyNoInteractions(userRankRepository, userAccountRedisRepository);
        }

        @Test
        @DisplayName("계좌가 없으면 초기 잔고로 계좌·랭크를 DB에 저장하고 Redis에 캐싱한다")
        void whenAccountNotExists_createsAccount() {
            given(userAccountRepository.existsByUsername(USERNAME)).willReturn(false);

            userAccountService.createAccountIfAbsent(USERNAME);

            ArgumentCaptor<UserAccountEntity> accountCaptor =
                    ArgumentCaptor.forClass(UserAccountEntity.class);
            verify(userAccountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getUsername()).isEqualTo(USERNAME);
            assertThat(accountCaptor.getValue().getBalance()).isEqualTo(INITIAL_BALANCE);

            ArgumentCaptor<UserRankEntity> rankCaptor =
                    ArgumentCaptor.forClass(UserRankEntity.class);
            verify(userRankRepository).save(rankCaptor.capture());
            assertThat(rankCaptor.getValue().getUsername()).isEqualTo(USERNAME);
            assertThat(rankCaptor.getValue().getPreviousDayTotalAsset()).isEqualTo(INITIAL_BALANCE);

            verify(userAccountRedisRepository).save(USERNAME,
                    UserAccountMessage.of(USERNAME, INITIAL_BALANCE, 0L, Map.of()));
        }
    }

    @Nested
    @DisplayName("settleAccounts")
    class SettleAccounts {

        private void givenDbBalance(long balance) {
            given(userAccountRepository.findByUsername(USERNAME))
                    .willReturn(Optional.of(UserAccountEntity.of(USERNAME, balance)));
        }

        private void givenRedisCash(Long availableCash, Long reservedCash) {
            given(userAccountRedisRepository.getAvailableCash(USERNAME)).willReturn(availableCash);
            given(userAccountRedisRepository.getReservedCash(USERNAME)).willReturn(reservedCash);
        }

        private void verifyNotSynced() {
            verify(userAccountRedisRepository, never()).updateAccountAfterClose(anyString(), anyLong());
            verify(accountUpdateEventPublisher, never()).publish(anyString());
        }

        @Test
        @DisplayName("DB에 계좌가 없으면 Redis를 조회하지 않고 건너뛴다")
        void whenAccountNotFound_skips() {
            given(userAccountRepository.findByUsername(USERNAME)).willReturn(Optional.empty());

            userAccountService.settleAccounts(Set.of(USERNAME));

            verifyNoInteractions(userAccountRedisRepository, accountUpdateEventPublisher);
        }

        @Test
        @DisplayName("DB 잔고와 Redis 합계가 같고 예약금이 0이면 동기화하지 않는다")
        void whenInSync_doesNothing() {
            givenDbBalance(10_000L);
            givenRedisCash(10_000L, 0L);

            userAccountService.settleAccounts(Set.of(USERNAME));

            verifyNotSynced();
        }

        @Test
        @DisplayName("DB 잔고와 Redis 합계가 다르면 Redis를 합계 기준으로 갱신하고 이벤트를 발행한다")
        void whenBalanceMismatch_syncs() {
            givenDbBalance(10_000L);
            givenRedisCash(7_000L, 0L);

            userAccountService.settleAccounts(Set.of(USERNAME));

            verify(userAccountRedisRepository).updateAccountAfterClose(USERNAME, 7_000L);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("합계가 같아도 예약금이 남아 있으면 예약금을 가용현금으로 풀어 동기화한다")
        void whenReservedCashRemains_syncs() {
            givenDbBalance(10_000L);
            givenRedisCash(8_000L, 2_000L);

            userAccountService.settleAccounts(Set.of(USERNAME));

            verify(userAccountRedisRepository).updateAccountAfterClose(USERNAME, 10_000L);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("Redis availableCash가 null이면 0으로 간주한다")
        void whenAvailableCashNull_treatsAsZero() {
            givenDbBalance(3_000L);
            givenRedisCash(null, 3_000L);

            userAccountService.settleAccounts(Set.of(USERNAME));

            verify(userAccountRedisRepository).updateAccountAfterClose(USERNAME, 3_000L);
            verify(accountUpdateEventPublisher).publish(USERNAME);
        }

        @Test
        @DisplayName("Redis reservedCash가 null이면 0으로 간주한다")
        void whenReservedCashNull_treatsAsZero() {
            givenDbBalance(5_000L);
            givenRedisCash(5_000L, null);

            userAccountService.settleAccounts(Set.of(USERNAME));

            verifyNotSynced();
        }
    }

    @Test
    @DisplayName("settleAllAccounts: 전체 username을 중복 없이 정산한다")
    void settleAllAccounts() {
        given(userAccountRepository.findAllUsernames()).willReturn(List.of(USERNAME, USERNAME));
        given(userAccountRepository.findByUsername(USERNAME)).willReturn(Optional.empty());

        userAccountService.settleAllAccounts();

        verify(userAccountRepository, times(1)).findByUsername(USERNAME);
    }
}
