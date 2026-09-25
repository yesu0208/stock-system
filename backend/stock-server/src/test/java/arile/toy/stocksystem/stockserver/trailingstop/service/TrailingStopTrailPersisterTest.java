package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.repository.TrailingStopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Persister] 트레일링 스탑 추적 상태 주기 저장 테스트")
@ExtendWith(MockitoExtension.class)
class TrailingStopTrailPersisterTest {

    @InjectMocks private TrailingStopTrailPersister sut;

    @Mock private TrailingStopRepository trailingStopRepository;

    @DisplayName("같은 트레일링 스탑이 여러 번 갱신되면 마지막 값만 ACTIVE 조건으로 한 번 저장한다")
    @Test
    void givenMultipleUpdates_whenFlushing_thenSavesLatestOnce() {
        sut.markDirty(dto(1L, 71_000, 68_800));
        sut.markDirty(dto(1L, 72_000, 69_800));

        sut.flush();

        then(trailingStopRepository).should(times(1))
                .updateTrail(1L, 72_000, 69_800, TrailingStopStatus.ACTIVE);
        assertThat(sut.pendingCount()).isZero();
    }

    @DisplayName("저장 대상이 없으면 DB를 호출하지 않는다")
    @Test
    void givenNothingDirty_whenFlushing_thenNoDbCall() {
        sut.flush();

        then(trailingStopRepository).shouldHaveNoInteractions();
    }

    @DisplayName("저장이 실패하면 남겨 두었다가 다음 주기에 다시 저장한다")
    @Test
    void givenSaveFails_whenFlushing_thenRetriesNextCycle() {
        sut.markDirty(dto(1L, 72_000, 69_800));
        given(trailingStopRepository.updateTrail(anyLong(), anyInt(), anyInt(), any()))
                .willThrow(new IllegalStateException("db error"))
                .willReturn(1);

        assertThatCode(() -> sut.flush()).doesNotThrowAnyException();
        assertThat(sut.pendingCount()).isEqualTo(1);

        sut.flush();
        assertThat(sut.pendingCount()).isZero();
        then(trailingStopRepository).should(times(2)).updateTrail(1L, 72_000, 69_800, TrailingStopStatus.ACTIVE);
    }

    @DisplayName("한 건 저장이 실패해도 다른 트레일링 스탑은 저장한다")
    @Test
    void givenOneFails_whenFlushing_thenOthersSaved() {
        sut.markDirty(dto(1L, 72_000, 69_800));
        sut.markDirty(dto(2L, 72_000, 69_800));
        // strict stub에서 인자별로 따로 stub하면 인자 불일치로 실패하므로 하나의 answer에서 분기
        given(trailingStopRepository.updateTrail(anyLong(), anyInt(), anyInt(), any()))
                .willAnswer(invocation -> {
                    if (invocation.getArgument(0, Long.class) == 1L) {
                        throw new IllegalStateException("db error");
                    }
                    return 1;
                });

        sut.flush();

        then(trailingStopRepository).should().updateTrail(2L, 72_000, 69_800, TrailingStopStatus.ACTIVE);
        assertThat(sut.pendingCount()).isEqualTo(1);
    }

    @DisplayName("저장하는 사이 더 새로운 값으로 갱신되면 새 값은 남겨 두고 다음 주기에 저장한다")
    @Test
    void givenUpdatedDuringFlush_whenFlushing_thenKeepsNewerValue() {
        sut.markDirty(dto(1L, 72_000, 69_800));
        given(trailingStopRepository.updateTrail(anyLong(), anyInt(), anyInt(), any()))
                .willAnswer(invocation -> {
                    // 첫 저장 도중 틱 처리에서 더 새로운 값으로 갱신된 상황
                    sut.markDirty(dto(1L, 73_000, 70_800));
                    return 1;
                })
                .willReturn(1);

        sut.flush();

        assertThat(sut.pendingCount()).isEqualTo(1);
        sut.flush();
        then(trailingStopRepository).should().updateTrail(1L, 73_000, 70_800, TrailingStopStatus.ACTIVE);
    }

    @DisplayName("정상 종료 시 남은 추적 상태를 저장한다")
    @Test
    void givenPending_whenShuttingDown_thenFlushes() {
        sut.markDirty(dto(1L, 72_000, 69_800));

        sut.flushOnShutdown();

        then(trailingStopRepository).should().updateTrail(1L, 72_000, 69_800, TrailingStopStatus.ACTIVE);
    }

    private TrailingStopDto dto(Long id, int basePrice, int triggerPrice) {
        return new TrailingStopDto(id, "user", "005930", TrailingStopType.SELL, LeverageRatio.SPOT,
                10, 3.0, basePrice, triggerPrice, 67_900, TrailingStopStatus.ACTIVE,
                Instant.parse("2026-09-25T00:00:00Z"));
    }
}
