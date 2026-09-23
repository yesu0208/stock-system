package arile.toy.stocksystem.accountserver.leverage.scheduler;

import arile.toy.stocksystem.accountserver.leverage.event.publisher.InterestAppliedPublisher;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageDailyBatchService;
import arile.toy.stocksystem.accountserver.market.holiday.HolidayRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DailyLeverageBatchSchedulerTest {

    private static final String LOCK_KEY = "lock:leverage:daily-batch";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private LeverageDailyBatchService leverageDailyBatchService;
    @Mock private InterestAppliedPublisher interestAppliedPublisher;
    @Mock private HolidayRegistry holidayRegistry;

    @InjectMocks
    private DailyLeverageBatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private void givenLock(Boolean acquired) {
        given(valueOps.setIfAbsent(LOCK_KEY, "LOCKED", Duration.ofMinutes(10))).willReturn(acquired);
    }

    @Test
    @DisplayName("등록된 휴장일이면 락을 잡지 않고 건너뛴다")
    void whenHoliday_skips() {
        given(holidayRegistry.isHoliday(any(LocalDate.class))).willReturn(true);

        scheduler.run();

        verifyNoInteractions(valueOps, leverageDailyBatchService, interestAppliedPublisher);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("다른 인스턴스가 락을 잡고 있으면 실행하지 않고, 남의 락을 지우지 않는다")
    void whenLockNotAcquired_skips() {
        given(holidayRegistry.isHoliday(any(LocalDate.class))).willReturn(false);
        givenLock(false);

        scheduler.run();

        verifyNoInteractions(leverageDailyBatchService, interestAppliedPublisher);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("setIfAbsent 결과가 null이면 락 획득 실패로 본다")
    void whenLockResultNull_skips() {
        given(holidayRegistry.isHoliday(any(LocalDate.class))).willReturn(false);
        givenLock(null);

        scheduler.run();

        verifyNoInteractions(leverageDailyBatchService);
    }

    @Test
    @DisplayName("락을 잡으면 배치 실행 → 이자 반영 알림 발행 → 락 해제 순으로 처리한다")
    void success_runsPublishesAndReleasesLock() {
        given(holidayRegistry.isHoliday(any(LocalDate.class))).willReturn(false);
        givenLock(true);

        scheduler.run();

        InOrder inOrder = inOrder(leverageDailyBatchService, interestAppliedPublisher, redisTemplate);
        inOrder.verify(leverageDailyBatchService).runDailyLeverageBatch();
        inOrder.verify(interestAppliedPublisher).publish();
        inOrder.verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("배치 실행 중 예외가 나도 락은 해제하고, 알림은 발행하지 않는다")
    void whenBatchFails_releasesLockWithoutPublishing() {
        given(holidayRegistry.isHoliday(any(LocalDate.class))).willReturn(false);
        givenLock(true);
        willThrow(new IllegalStateException("batch failed")).given(leverageDailyBatchService).runDailyLeverageBatch();

        assertThatThrownBy(() -> scheduler.run())
                .isInstanceOf(IllegalStateException.class);

        verify(redisTemplate).delete(LOCK_KEY);
        verifyNoInteractions(interestAppliedPublisher);
    }
}
