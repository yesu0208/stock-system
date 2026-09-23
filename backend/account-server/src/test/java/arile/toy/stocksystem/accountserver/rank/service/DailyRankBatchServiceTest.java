package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.service.DailyRankExecutor.DailyRankOutcome;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DailyRankBatchServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private DailyRankExecutor dailyRankExecutor;

    @InjectMocks
    private DailyRankBatchService service;

    @Test
    @DisplayName("계좌가 없으면 실행기를 호출하지 않는다")
    void whenNoAccounts_doesNothing() {
        given(userAccountRepository.findAll()).willReturn(List.of());

        service.runDailyBatch();

        verifyNoInteractions(dailyRankExecutor);
    }

    @Test
    @DisplayName("모든 계좌를 현금 잔고와 함께 실행기로 넘기고, 한 명이 실패해도 나머지를 계속 처리한다")
    void processesAllAndContinuesOnFailure() {
        LocalDate today = LocalDate.now();
        given(userAccountRepository.findAll()).willReturn(List.of(
                UserAccountEntity.of("ranked", 1_000L),
                UserAccountEntity.of("unranked", 2_000L),
                UserAccountEntity.of("failing", 3_000L),
                UserAccountEntity.of("after", 4_000L)));
        given(dailyRankExecutor.processOneUser("ranked", 1_000L, today)).willReturn(DailyRankOutcome.RANKED);
        given(dailyRankExecutor.processOneUser("unranked", 2_000L, today))
                .willReturn(DailyRankOutcome.UNRANKED_SNAPSHOT);
        given(dailyRankExecutor.processOneUser("failing", 3_000L, today))
                .willThrow(new IllegalStateException("db down"));
        given(dailyRankExecutor.processOneUser("after", 4_000L, today)).willReturn(DailyRankOutcome.SKIPPED);

        service.runDailyBatch();

        verify(dailyRankExecutor).processOneUser("after", 4_000L, today);
    }
}
