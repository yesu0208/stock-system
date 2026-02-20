package arile.toy.stocksystem.stockserver.useraccount.service;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;
import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.StockServerAccountRepository;
import arile.toy.stocksystem.stockserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StockServerAccountRepository stockServerAccountRepository;

    @Mock
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    @DisplayName("새로운 사용자를 생성하면 계정이 생성된다")
    void givenNewUser_whenCreateAccountIfAbsent_thenAccountCreated() {

        String username = "testUser";

        when(userAccountRepository.existsByUsername(username))
                .thenReturn(false);

        userAccountService.createAccountIfAbsent(username);

        verify(userAccountRepository).save(any(UserAccountEntity.class));
        verify(stockServerAccountRepository).save(
                eq(username),
                any(StockServerAccountMessage.class)
        );
    }

    @Test
    @DisplayName("이미 존재하는 사용자를 생성하면 아무 작업도 수행하지 않는다")
    void givenDuplicateUser_whenCreateAccountIfAbsent_thenDoNothing() {

        String username = "testUser";

        when(userAccountRepository.existsByUsername(username))
                .thenReturn(true);

        userAccountService.createAccountIfAbsent(username);

        verify(userAccountRepository, never()).save(any());
        verify(stockServerAccountRepository, never()).save(anyString(), any());
    }

    @Test
    @DisplayName("잔액이 불일치하면 정산 후 업데이트하고 이벤트 발행")
    void givenBalanceOutOfSync_whenSettleAccounts_thenUpdateAndPublish() {

        String username = "user1";

        UserAccountEntity entity =
                UserAccountEntity.of(username, 1000L);

        when(userAccountRepository.findByUsername(username))
                .thenReturn(Optional.of(entity));

        when(stockServerAccountRepository.getAvailableCash(username))
                .thenReturn(800L);

        when(stockServerAccountRepository.getReservedCash(username))
                .thenReturn(300L); // total = 1100

        userAccountService.settleAccounts(Set.of(username));

        verify(stockServerAccountRepository)
                .updateAccountAfterClose(username, 1100L);

        verify(accountUpdateEventPublisher)
                .publish(username);
    }

    @Test
    @DisplayName("잔액이 일치하면 정산 시 아무 작업도 수행하지 않는다")
    void givenBalanceInSync_whenSettleAccounts_thenDoNothing() {

        String username = "user1";

        UserAccountEntity entity =
                UserAccountEntity.of(username, 1000L);

        when(userAccountRepository.findByUsername(username))
                .thenReturn(Optional.of(entity));

        when(stockServerAccountRepository.getAvailableCash(username))
                .thenReturn(1000L);

        when(stockServerAccountRepository.getReservedCash(username))
                .thenReturn(0L);

        userAccountService.settleAccounts(Set.of(username));

        verify(stockServerAccountRepository, never())
                .updateAccountAfterClose(anyString(), anyLong());

        verify(accountUpdateEventPublisher, never())
                .publish(anyString());
    }

    @Test
    @DisplayName("계정이 존재하지 않으면 정산 시 스킵한다")
    void givenAccountNotFound_whenSettleAccounts_thenSkip() {

        String username = "missingUser";

        when(userAccountRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        userAccountService.settleAccounts(Set.of(username));

        verifyNoInteractions(stockServerAccountRepository);
        verifyNoInteractions(accountUpdateEventPublisher);
    }

    @Test
    @DisplayName("Redis에서 null 값이 반환되면 0으로 처리하고 업데이트하지 않는다")
    void givenNullRedisValues_whenSettleAccounts_thenTreatAsZero() {

        String username = "user1";

        UserAccountEntity entity =
                UserAccountEntity.of(username, 0L);

        when(userAccountRepository.findByUsername(username))
                .thenReturn(Optional.of(entity));

        when(stockServerAccountRepository.getAvailableCash(username))
                .thenReturn(null);

        when(stockServerAccountRepository.getReservedCash(username))
                .thenReturn(null);

        userAccountService.settleAccounts(Set.of(username));

        verify(stockServerAccountRepository, never())
                .updateAccountAfterClose(anyString(), anyLong());
    }
}
