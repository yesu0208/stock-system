package arile.toy.stocksystem.stockserver.market.holiday.controller;

import arile.toy.stocksystem.stockserver.market.holiday.entity.MarketHolidayEntity;
import arile.toy.stocksystem.stockserver.market.holiday.service.MarketHolidayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[Controller] 휴장일 내부 API 테스트")
@WebMvcTest(InternalMarketHolidayController.class)
class InternalMarketHolidayControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private MarketHolidayService marketHolidayService;

    @DisplayName("휴장일 목록을 날짜·메모로 반환한다")
    @Test
    void whenGettingHolidays_thenReturnsList() throws Exception {
        given(marketHolidayService.getAll()).willReturn(List.of(
                MarketHolidayEntity.of(LocalDate.of(2026, 10, 5), "추석 대체")));

        mvc.perform(get("/internal/market/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].holidayDate").value("2026-10-05"))
                .andExpect(jsonPath("$[0].memo").value("추석 대체"));
    }

    @DisplayName("휴장일을 등록하면 201과 등록된 휴장일을 반환한다")
    @Test
    void givenValidRequest_whenAdding_thenCreated() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 31);
        given(marketHolidayService.addHoliday(date, "연말 휴장"))
                .willReturn(MarketHolidayEntity.of(date, "연말 휴장"));

        mvc.perform(post("/internal/market/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"holidayDate":"2026-12-31","memo":"연말 휴장"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holidayDate").value("2026-12-31"))
                .andExpect(jsonPath("$.memo").value("연말 휴장"));
    }

    @DisplayName("날짜가 없으면 400을 반환하고 서비스를 호출하지 않는다")
    @Test
    void givenMissingDate_whenAdding_thenBadRequest() throws Exception {
        mvc.perform(post("/internal/market/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memo":"날짜 없음"}
                                """))
                .andExpect(status().isBadRequest());

        then(marketHolidayService).should(never()).addHoliday(any(), any());
    }

    @DisplayName("휴장일을 삭제하면 204를 반환한다")
    @Test
    void givenDate_whenRemoving_thenNoContent() throws Exception {
        mvc.perform(delete("/internal/market/holidays/{date}", "2026-12-31"))
                .andExpect(status().isNoContent());

        then(marketHolidayService).should().removeHoliday(LocalDate.of(2026, 12, 31));
    }
}
