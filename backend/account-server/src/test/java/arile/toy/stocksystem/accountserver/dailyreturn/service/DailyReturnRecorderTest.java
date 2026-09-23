package arile.toy.stocksystem.accountserver.dailyreturn.service;

import arile.toy.stocksystem.accountserver.dailyreturn.entity.DailyReturnHistoryEntity;
import arile.toy.stocksystem.accountserver.dailyreturn.repository.DailyReturnHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyReturnRecorderTest {

    private static final String USERNAME = "user1";
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final long INITIAL_BALANCE = 1_000_000_000L;

    @Mock
    private DailyReturnHistoryRepository dailyReturnHistoryRepository;

    @InjectMocks
    private DailyReturnRecorder recorder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recorder, "initialBalance", INITIAL_BALANCE);
    }

    private DailyReturnHistoryEntity recordAndCapture(long previous, long today, long tradeAmount) {
        given(dailyReturnHistoryRepository.existsByUsernameAndRecordDate(USERNAME, TODAY)).willReturn(false);

        recorder.record(USERNAME, TODAY, previous, today, tradeAmount);

        ArgumentCaptor<DailyReturnHistoryEntity> captor = ArgumentCaptor.forClass(DailyReturnHistoryEntity.class);
        verify(dailyReturnHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("당일 손익·수익률과 초기 자본 대비 누적 손익·수익률을 계산해 저장한다")
    void recordsProfit() {
        DailyReturnHistoryEntity saved =
                recordAndCapture(1_000_000_000L, 1_050_000_000L, 30_000_000L);

        assertThat(saved.getUsername()).isEqualTo(USERNAME);
        assertThat(saved.getRecordDate()).isEqualTo(TODAY);
        assertThat(saved.getPreviousDayTotalAsset()).isEqualTo(1_000_000_000L);
        assertThat(saved.getTotalAsset()).isEqualTo(1_050_000_000L);
        assertThat(saved.getDailyProfitAmount()).isEqualTo(50_000_000L);
        assertThat(saved.getDailyProfitRate()).isCloseTo(5.0, within(1e-9));
        assertThat(saved.getDailyTradeAmount()).isEqualTo(30_000_000L);
        assertThat(saved.getCumulativeProfitAmount()).isEqualTo(50_000_000L);
        assertThat(saved.getCumulativeProfitRate()).isCloseTo(5.0, within(1e-9));
    }

    @Test
    @DisplayName("손실이면 당일·누적 손익과 수익률이 음수로 기록된다")
    void recordsLoss() {
        // 전일 1,100,000,000 → 당일 990,000,000 (당일 -10%, 누적 -1%)
        DailyReturnHistoryEntity saved =
                recordAndCapture(1_100_000_000L, 990_000_000L, 0L);

        assertThat(saved.getDailyProfitAmount()).isEqualTo(-110_000_000L);
        assertThat(saved.getDailyProfitRate()).isCloseTo(-10.0, within(1e-9));
        assertThat(saved.getCumulativeProfitAmount()).isEqualTo(-10_000_000L);
        assertThat(saved.getCumulativeProfitRate()).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    @DisplayName("전일 총자산이 0이면 당일 수익률을 0으로 기록한다")
    void whenPreviousAssetZero_dailyRateIsZero() {
        DailyReturnHistoryEntity saved = recordAndCapture(0L, 1_000_000_000L, 0L);

        assertThat(saved.getDailyProfitAmount()).isEqualTo(1_000_000_000L);
        assertThat(saved.getDailyProfitRate()).isZero();
    }

    @Test
    @DisplayName("같은 날짜의 기록이 이미 있으면 저장하지 않는다")
    void whenAlreadyRecorded_skips() {
        given(dailyReturnHistoryRepository.existsByUsernameAndRecordDate(USERNAME, TODAY)).willReturn(true);

        recorder.record(USERNAME, TODAY, 1_000_000_000L, 1_050_000_000L, 0L);

        verify(dailyReturnHistoryRepository, never()).save(any());
    }
}
