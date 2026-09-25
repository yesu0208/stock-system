package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoHistoryItem;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
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

@DisplayName("[Service] OTOCO 이력 조회 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoHistoryQueryServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @InjectMocks private OtocoHistoryQueryService sut;
    @Mock private OtocoRepository otocoRepository;

    @DisplayName("전체 이력은 상태 조건 없이 조회하고 페이지 응답으로 변환한다")
    @Test
    void whenGettingHistory_thenNoStatusFilter() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-30T00:00:00Z");
        given(otocoRepository.search("user", "005930", null, from, to, PAGEABLE))
                .willReturn(new PageImpl<>(List.of(OtocoFixtures.entity(OtocoStatus.WAITING_EXIT)), PAGEABLE, 21));

        HistoryPageResponse<OtocoHistoryItem> result = sut.getHistory("user", "005930", from, to, PAGEABLE);

        assertThat(result.items()).extracting(OtocoHistoryItem::otocoId).containsExactly(1L);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.hasNext()).isTrue();
    }

    @DisplayName("취소는 CANCELED, 미완료는 진행 중 상태 전체, 완료는 COMPLETED로 조회한다")
    @Test
    void whenGettingByStatus_thenFiltersStatus() {
        given(otocoRepository.search(eq("user"), isNull(), anyList(), isNull(), isNull(), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        sut.getCancelHistory("user", null, null, null, PAGEABLE);
        sut.getUnfilled("user", null, null, null, PAGEABLE);
        sut.getCompletedHistory("user", null, null, null, PAGEABLE);

        then(otocoRepository).should().search("user", null, List.of(OtocoStatus.CANCELED), null, null, PAGEABLE);
        then(otocoRepository).should().search("user", null,
                List.of(OtocoStatus.WAITING_ENTRY, OtocoStatus.ENTRY_ORDER_PLACED, OtocoStatus.WAITING_EXIT),
                null, null, PAGEABLE);
        then(otocoRepository).should().search("user", null, List.of(OtocoStatus.COMPLETED), null, null, PAGEABLE);
    }
}
