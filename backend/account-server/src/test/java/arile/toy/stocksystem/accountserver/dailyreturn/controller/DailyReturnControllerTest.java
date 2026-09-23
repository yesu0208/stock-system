package arile.toy.stocksystem.accountserver.dailyreturn.controller;

import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryItem;
import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryResponse;
import arile.toy.stocksystem.accountserver.dailyreturn.service.DailyReturnQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DailyReturnController.class)
@ActiveProfiles("test")
class DailyReturnControllerTest {

    private static final String USERNAME = "user1";
    private static final String URL = "/internal/returns/{username}/history";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyReturnQueryService dailyReturnQueryService;

    @Test
    @DisplayName("파라미터가 없으면 전체 기간, 0페이지, 20개로 조회한다")
    void defaultParams() throws Exception {
        given(dailyReturnQueryService.getHistory(USERNAME, null, null, 0, 20))
                .willReturn(new DailyReturnHistoryResponse(List.of(), false));

        mockMvc.perform(get(URL, USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("ISO 날짜 형식의 기간과 페이지 조건을 서비스로 넘기고 이력을 반환한다")
    void withParams() throws Exception {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        given(dailyReturnQueryService.getHistory(USERNAME, from, to, 1, 5))
                .willReturn(new DailyReturnHistoryResponse(List.of(new DailyReturnHistoryItem(
                        LocalDate.of(2026, 9, 24), 1_000_000_000L, 1_050_000_000L, 50_000_000L, 5.0,
                        30_000_000L, 50_000_000L, 5.0)), true));

        mockMvc.perform(get(URL, USERNAME)
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].date").value("2026-09-24"))
                .andExpect(jsonPath("$.items[0].totalAsset").value(1_050_000_000L))
                .andExpect(jsonPath("$.items[0].dailyProfitRate").value(5.0))
                .andExpect(jsonPath("$.items[0].cumulativeProfitAmount").value(50_000_000L))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("날짜 형식이 ISO가 아니면 400을 반환하고 조회하지 않는다")
    void invalidDate_returnsBadRequest() throws Exception {
        mockMvc.perform(get(URL, USERNAME).param("from", "2026/09/01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dailyReturnQueryService);
    }
}
