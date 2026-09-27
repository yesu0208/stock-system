package arile.toy.stocksystem.bffserver.market.holiday.controller;

import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCalendarResponse;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayResponse;
import arile.toy.stocksystem.bffserver.market.holiday.service.MarketHolidayQueryService;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketHolidayController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class MarketHolidayControllerTest {

    private static final String BASE = "/api/v1/market/holidays";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private MarketHolidayQueryService marketHolidayQueryService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인한 일반 사용자는 오늘~마지막 휴장일 범위를 조회한다")
    void getUpcomingHolidays() throws Exception {
        given(marketHolidayQueryService.getUpcomingHolidays()).willReturn(MarketHolidayCalendarResponse.of(
                LocalDate.of(2026, 10, 1),
                List.of(new MarketHolidayResponse(LocalDate.of(2026, 10, 9), "한글날"))));

        mockMvc.perform(get(BASE).with(user("user1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-10-01"))
                .andExpect(jsonPath("$.lastHolidayDate").value("2026-10-09"))
                .andExpect(jsonPath("$.holidays[0].holidayDate").value("2026-10-09"))
                .andExpect(jsonPath("$.holidays[0].memo").value("한글날"));
    }

    @Test
    @DisplayName("비로그인은 401이고 조회하지 않는다")
    void anonymous() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verifyNoInteractions(marketHolidayQueryService);
    }
}
