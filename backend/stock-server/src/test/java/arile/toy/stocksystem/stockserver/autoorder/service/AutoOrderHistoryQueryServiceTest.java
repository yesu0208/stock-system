package arile.toy.stocksystem.stockserver.autoorder.service;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 자동주문 내역 조회 테스트")
@ExtendWith(MockitoExtension.class)
class AutoOrderHistoryQueryServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-25T00:00:00Z");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @InjectMocks private AutoOrderHistoryQueryService sut;

    @Mock private AutoOrderRepository autoOrderRepository;

    @DisplayName("전체 내역은 상태 조건 없이(null) 조회하고 내역 항목 페이지로 변환한다")
    @Test
    void whenGettingHistory_thenSearchesWithoutStatusFilter() {
        AutoOrderEntity entity = AutoOrderEntity.of(USERNAME, STOCK_CODE, AutoOrderType.BUY, LeverageRatio.SPOT,
                71_000, 70_000, 10, AutoOrderStatus.TRIGGERED);
        entity.setAutoOrderId(1L);
        given(autoOrderRepository.search(eq(USERNAME), eq(STOCK_CODE), isNull(), eq(FROM), eq(TO), eq(PAGEABLE)))
                .willReturn(new PageImpl<>(List.of(entity), PAGEABLE, 1));

        HistoryPageResponse<AutoOrderHistoryItem> result =
                sut.getHistory(USERNAME, STOCK_CODE, FROM, TO, PAGEABLE);

        assertThat(result.items()).singleElement()
                .extracting(AutoOrderHistoryItem::autoOrderId).isEqualTo(1L);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("취소 내역은 CANCELED로만 조회한다")
    @Test
    void whenGettingCancelHistory_thenSearchesCanceled() {
        given(autoOrderRepository.search(USERNAME, null, List.of(AutoOrderStatus.CANCELED), null, null, PAGEABLE))
                .willReturn(Page.empty(PAGEABLE));

        assertThat(sut.getCancelHistory(USERNAME, null, null, null, PAGEABLE).items()).isEmpty();
    }

    @DisplayName("미발동 자동주문은 열린 상태(ACTIVE)로만 조회한다")
    @Test
    void whenGettingUnfilled_thenSearchesActive() {
        given(autoOrderRepository.search(USERNAME, null, List.of(AutoOrderStatus.ACTIVE), null, null, PAGEABLE))
                .willReturn(Page.empty(PAGEABLE));

        assertThat(sut.getUnfilled(USERNAME, null, null, null, PAGEABLE).items()).isEmpty();
    }

    @DisplayName("발동 내역은 TRIGGERED로만 조회한다")
    @Test
    void whenGettingTriggeredHistory_thenSearchesTriggered() {
        given(autoOrderRepository.search(USERNAME, null, List.of(AutoOrderStatus.TRIGGERED), null, null, PAGEABLE))
                .willReturn(Page.empty(PAGEABLE));

        assertThat(sut.getTriggeredHistory(USERNAME, null, null, null, PAGEABLE).items()).isEmpty();
    }
}
