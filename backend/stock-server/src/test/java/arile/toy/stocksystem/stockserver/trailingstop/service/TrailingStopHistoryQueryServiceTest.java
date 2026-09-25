package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopHistoryItem;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.repository.TrailingStopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 트레일링 스탑 이력 조회 테스트")
@ExtendWith(MockitoExtension.class)
class TrailingStopHistoryQueryServiceTest {

    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-30T00:00:00Z");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @InjectMocks private TrailingStopHistoryQueryService sut;
    @Mock private TrailingStopRepository trailingStopRepository;

    @DisplayName("전체 이력은 상태 조건 없이 조회하고 페이지 응답으로 변환한다")
    @Test
    void whenGettingHistory_thenNoStatusFilter() {
        TrailingStopEntity entity = TrailingStopEntity.of("user", "005930", TrailingStopType.BUY,
                LeverageRatio.SPOT, 10, 3.0, 70_000, 72_100, TrailingStopStatus.ACTIVE);
        entity.setTrailingStopId(1L);
        given(trailingStopRepository.search("user", "005930", null, FROM, TO, PAGEABLE))
                .willReturn(new PageImpl<>(List.of(entity), PAGEABLE, 21));

        HistoryPageResponse<TrailingStopHistoryItem> result = sut.getHistory("user", "005930", FROM, TO, PAGEABLE);

        assertThat(result.items()).extracting(TrailingStopHistoryItem::trailingStopId).containsExactly(1L);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.hasNext()).isTrue();
    }

    @DisplayName("취소 이력은 CANCELED, 미발동은 ACTIVE, 발동 이력은 TRIGGERED로 조회한다")
    @Test
    void whenGettingByStatus_thenFiltersStatus() {
        given(trailingStopRepository.search(eq("user"), isNull(), anyList(), isNull(), isNull(), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        sut.getCancelHistory("user", null, null, null, PAGEABLE);
        sut.getUnfilled("user", null, null, null, PAGEABLE);
        sut.getTriggeredHistory("user", null, null, null, PAGEABLE);

        then(trailingStopRepository).should().search("user", null, List.of(TrailingStopStatus.CANCELED), null, null, PAGEABLE);
        then(trailingStopRepository).should().search("user", null, List.of(TrailingStopStatus.ACTIVE), null, null, PAGEABLE);
        then(trailingStopRepository).should().search("user", null, List.of(TrailingStopStatus.TRIGGERED), null, null, PAGEABLE);
    }
}
