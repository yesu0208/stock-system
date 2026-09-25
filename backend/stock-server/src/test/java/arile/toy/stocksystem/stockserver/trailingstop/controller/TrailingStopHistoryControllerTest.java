package arile.toy.stocksystem.stockserver.trailingstop.controller;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopHistoryItem;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopHistoryQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[Controller] 트레일링 스탑 이력 조회 API 테스트")
@WebMvcTest(TrailingStopHistoryController.class)
class TrailingStopHistoryControllerTest {

    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-30T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private TrailingStopHistoryQueryService trailingStopHistoryQueryService;

    @DisplayName("전체 이력: 파라미터를 전달하고 페이지 응답을 반환한다")
    @Test
    void whenGettingHistory_thenReturnsPage() throws Exception {
        var item = new TrailingStopHistoryItem(1L, "005930", TrailingStopType.SELL, LeverageRatio.SPOT,
                10, 3.0, 72_000, 69_800, TrailingStopStatus.ACTIVE, FROM);
        given(trailingStopHistoryQueryService.getHistory("user", "005930", FROM, TO, PageRequest.of(1, 5)))
                .willReturn(new HistoryPageResponse<>(List.of(item), 1, 5, 6, false));

        mvc.perform(get("/internal/trailing-stops/user/history")
                        .param("stockCode", "005930")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].trailingStopId").value(1))
                .andExpect(jsonPath("$.items[0].basePrice").value(72000))
                .andExpect(jsonPath("$.items[0].triggerPrice").value(69800))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @DisplayName("취소·미발동·발동 이력: 파라미터가 없으면 기본 페이지(0, 20)로 각 조회에 위임한다")
    @Test
    void whenGettingOthers_thenDelegatesWithDefaults() throws Exception {
        var empty = new HistoryPageResponse<TrailingStopHistoryItem>(List.of(), 0, 20, 0, false);
        var pageable = PageRequest.of(0, 20);
        given(trailingStopHistoryQueryService.getCancelHistory("user", null, null, null, pageable)).willReturn(empty);
        given(trailingStopHistoryQueryService.getUnfilled("user", null, null, null, pageable)).willReturn(empty);
        given(trailingStopHistoryQueryService.getTriggeredHistory("user", null, null, null, pageable)).willReturn(empty);

        mvc.perform(get("/internal/trailing-stops/user/cancels")).andExpect(status().isOk());
        mvc.perform(get("/internal/trailing-stops/user/unfilled")).andExpect(status().isOk());
        mvc.perform(get("/internal/trailing-stops/user/triggered")).andExpect(status().isOk());

        then(trailingStopHistoryQueryService).should().getCancelHistory("user", null, null, null, pageable);
        then(trailingStopHistoryQueryService).should().getUnfilled("user", null, null, null, pageable);
        then(trailingStopHistoryQueryService).should().getTriggeredHistory("user", null, null, null, pageable);
    }
}
