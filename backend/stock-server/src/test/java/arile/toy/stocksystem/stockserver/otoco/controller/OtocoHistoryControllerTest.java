package arile.toy.stocksystem.stockserver.otoco.controller;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoHistoryQueryService;
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

@DisplayName("[Controller] OTOCO 이력 조회 API 테스트")
@WebMvcTest(OtocoHistoryController.class)
class OtocoHistoryControllerTest {

    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-30T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private OtocoHistoryQueryService otocoHistoryQueryService;

    @DisplayName("전체 이력: 파라미터를 전달하고 페이지 응답을 반환한다")
    @Test
    void whenGettingHistory_thenReturnsPage() throws Exception {
        var item = new OtocoHistoryItem(1L, "005930", OtocoEntryDirection.BELOW, LeverageRatio.SPOT, 10,
                70_000, 73_500, 67_900, OtocoStatus.COMPLETED, OtocoLeg.TAKE_PROFIT, FROM);
        given(otocoHistoryQueryService.getHistory("user", "005930", FROM, TO, PageRequest.of(1, 5)))
                .willReturn(new HistoryPageResponse<>(List.of(item), 1, 5, 6, false));

        mvc.perform(get("/internal/otocos/user/history")
                        .param("stockCode", "005930")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].otocoId").value(1))
                .andExpect(jsonPath("$.items[0].completedLeg").value("TAKE_PROFIT"))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @DisplayName("취소·미완료·완료 이력: 기본 페이지(0, 20)로 각 조회에 위임한다")
    @Test
    void whenGettingOthers_thenDelegatesWithDefaults() throws Exception {
        var empty = new HistoryPageResponse<OtocoHistoryItem>(List.of(), 0, 20, 0, false);
        var pageable = PageRequest.of(0, 20);
        given(otocoHistoryQueryService.getCancelHistory("user", null, null, null, pageable)).willReturn(empty);
        given(otocoHistoryQueryService.getUnfilled("user", null, null, null, pageable)).willReturn(empty);
        given(otocoHistoryQueryService.getCompletedHistory("user", null, null, null, pageable)).willReturn(empty);

        mvc.perform(get("/internal/otocos/user/cancels")).andExpect(status().isOk());
        mvc.perform(get("/internal/otocos/user/unfilled")).andExpect(status().isOk());
        mvc.perform(get("/internal/otocos/user/completed")).andExpect(status().isOk());

        then(otocoHistoryQueryService).should().getCancelHistory("user", null, null, null, pageable);
        then(otocoHistoryQueryService).should().getUnfilled("user", null, null, null, pageable);
        then(otocoHistoryQueryService).should().getCompletedHistory("user", null, null, null, pageable);
    }
}
