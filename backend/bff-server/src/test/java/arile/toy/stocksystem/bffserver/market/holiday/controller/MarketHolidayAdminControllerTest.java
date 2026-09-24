package arile.toy.stocksystem.bffserver.market.holiday.controller;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.market.holiday.client.MarketHolidayApiClient;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCreateRequest;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketHolidayAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class MarketHolidayAdminControllerTest {

    private static final String BASE = "/api/v1/admin/market/holidays";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private MarketHolidayApiClient marketHolidayApiClient;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"GET, /api/v1/admin/market/holidays", "POST, /api/v1/admin/market/holidays",
            "DELETE, /api/v1/admin/market/holidays/2026-10-09"})
    @DisplayName("일반 사용자는 403, 비로그인은 401이고 stock-server를 호출하지 않는다")
    void nonAdmin(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(user("user1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"holidayDate\": \"2026-10-09\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"holidayDate\": \"2026-10-09\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(marketHolidayApiClient);
    }

    @Test
    @DisplayName("목록을 조회한다")
    void getHolidays() throws Exception {
        given(marketHolidayApiClient.getHolidays()).willReturn(List.of());

        mockMvc.perform(get(BASE).with(admin())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("등록: 날짜·메모를 넘기고 201을 반환한다")
    void addHoliday() throws Exception {
        mockMvc.perform(post(BASE).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holidayDate\": \"2026-10-09\", \"memo\": \"한글날\"}"))
                .andExpect(status().isCreated());

        verify(marketHolidayApiClient).addHoliday(new MarketHolidayCreateRequest(LocalDate.of(2026, 10, 9), "한글날"));
    }

    @Test
    @DisplayName("등록: 날짜가 없거나 형식이 틀리면 400이다")
    void addHoliday_invalid() throws Exception {
        mockMvc.perform(post(BASE).with(admin()).contentType(MediaType.APPLICATION_JSON).content("{\"memo\": \"x\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holidayDate\": \"2026/10/09\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marketHolidayApiClient);
    }

    @Test
    @DisplayName("등록: 중복 등록(409)은 그 상태로 응답하고 Slack 알림을 보내지 않는다")
    void addHoliday_conflict() throws Exception {
        given(marketHolidayApiClient.addHoliday(new MarketHolidayCreateRequest(LocalDate.of(2026, 10, 9), null)))
                .willThrow(new ClientErrorException(HttpStatus.CONFLICT, "휴장일 요청을 처리할 수 없습니다."));

        mockMvc.perform(post(BASE).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holidayDate\": \"2026-10-09\"}"))
                .andExpect(status().isConflict());

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("삭제: 날짜를 넘기고 204를 반환한다. 날짜 형식이 틀리면 400이다")
    void removeHoliday() throws Exception {
        mockMvc.perform(delete(BASE + "/2026-10-09").with(admin())).andExpect(status().isNoContent());
        mockMvc.perform(delete(BASE + "/not-a-date").with(admin())).andExpect(status().isBadRequest());

        verify(marketHolidayApiClient).removeHoliday(LocalDate.of(2026, 10, 9));
    }

    @Test
    @DisplayName("삭제: stock-server 장애면 503으로 응답한다")
    void removeHoliday_unavailable() throws Exception {
        willThrow(new ClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "휴장일 서버에 연결할 수 없습니다."))
                .given(marketHolidayApiClient).removeHoliday(LocalDate.of(2026, 10, 9));

        mockMvc.perform(delete(BASE + "/2026-10-09").with(admin())).andExpect(status().isServiceUnavailable());

        verifyNoInteractions(slackNotifier);
    }
}
