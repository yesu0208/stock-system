package arile.toy.stocksystem.stockserver.autoorder.controller;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderHistoryQueryService;
import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
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

@DisplayName("[Controller] 자동주문 내역 내부 API 테스트")
@WebMvcTest(AutoOrderHistoryController.class)
class AutoOrderHistoryControllerTest {

    private static final String USERNAME = "user";
    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 20);

    @Autowired private MockMvc mvc;

    @MockitoBean private AutoOrderHistoryQueryService autoOrderHistoryQueryService;

    @DisplayName("전체 내역: 종목·ISO 기간·페이지 파라미터를 서비스에 넘기고 페이지 응답을 반환한다")
    @Test
    void givenAllParams_whenGettingHistory_thenPassesParams() throws Exception {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-25T00:00:00Z");
        given(autoOrderHistoryQueryService.getHistory(USERNAME, "005930", from, to, PageRequest.of(1, 5)))
                .willReturn(new HistoryPageResponse<>(List.of(), 1, 5, 6L, false));

        mvc.perform(get("/internal/auto-orders/{username}/history", USERNAME)
                        .param("stockCode", "005930")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-25T00:00:00Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @DisplayName("취소 내역: 기본값(null 조건, 0/20 페이지)으로 취소 내역 조회에 위임한다")
    @Test
    void whenGettingCancels_thenDelegates() throws Exception {
        given(autoOrderHistoryQueryService.getCancelHistory(USERNAME, null, null, null, DEFAULT_PAGE))
                .willReturn(empty());

        mvc.perform(get("/internal/auto-orders/{username}/cancels", USERNAME))
                .andExpect(status().isOk());

        then(autoOrderHistoryQueryService).should().getCancelHistory(USERNAME, null, null, null, DEFAULT_PAGE);
    }

    @DisplayName("미발동: 미발동 조회에 위임한다")
    @Test
    void whenGettingUnfilled_thenDelegates() throws Exception {
        given(autoOrderHistoryQueryService.getUnfilled(USERNAME, null, null, null, DEFAULT_PAGE))
                .willReturn(empty());

        mvc.perform(get("/internal/auto-orders/{username}/unfilled", USERNAME))
                .andExpect(status().isOk());

        then(autoOrderHistoryQueryService).should().getUnfilled(USERNAME, null, null, null, DEFAULT_PAGE);
    }

    @DisplayName("발동 내역: 발동 내역 조회에 위임한다")
    @Test
    void whenGettingTriggered_thenDelegates() throws Exception {
        given(autoOrderHistoryQueryService.getTriggeredHistory(USERNAME, null, null, null, DEFAULT_PAGE))
                .willReturn(empty());

        mvc.perform(get("/internal/auto-orders/{username}/triggered", USERNAME))
                .andExpect(status().isOk());

        then(autoOrderHistoryQueryService).should().getTriggeredHistory(USERNAME, null, null, null, DEFAULT_PAGE);
    }

    @DisplayName("기간 파라미터 형식이 잘못되면 400을 반환한다")
    @Test
    void givenInvalidDate_whenGettingHistory_thenBadRequest() throws Exception {
        mvc.perform(get("/internal/auto-orders/{username}/history", USERNAME).param("to", "yesterday"))
                .andExpect(status().isBadRequest());

        then(autoOrderHistoryQueryService).shouldHaveNoInteractions();
    }

    private HistoryPageResponse<AutoOrderHistoryItem> empty() {
        return new HistoryPageResponse<>(List.of(), 0, 20, 0L, false);
    }
}
