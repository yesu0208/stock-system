package arile.toy.stocksystem.bffserver.dailyreturn.controller;

import arile.toy.stocksystem.bffserver.admin.service.AdminAccessService;
import arile.toy.stocksystem.bffserver.dailyreturn.client.DailyReturnApiClient;
import arile.toy.stocksystem.bffserver.dailyreturn.dto.DailyReturnHistoryResponse;
import arile.toy.stocksystem.bffserver.exception.admin.AdminAccessDeniedException;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DailyReturnController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, AdminAccessService.class})
@ActiveProfiles("test")
class DailyReturnControllerTest {

    private static final String URL = "/api/v1/users/returns/history";
    private static final DailyReturnHistoryResponse RESPONSE = new DailyReturnHistoryResponse(List.of(), true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DailyReturnApiClient dailyReturnApiClient;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Autowired
    private DailyReturnController controller;

    @Test
    @DisplayName("본인 이력: 로그인 사용자 기준으로 기간·페이지를 넘겨 조회한다")
    void ownHistory() throws Exception {
        given(dailyReturnApiClient.getHistory("user1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 24), 1, 20))
                .willReturn(RESPONSE);

        mockMvc.perform(get(URL).with(user("user1"))
                        .param("from", "2026-09-01").param("to", "2026-09-24")
                        .param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("페이지·개수를 생략하면 0페이지·20개로 조회한다")
    void defaultPaging() throws Exception {
        given(dailyReturnApiClient.getHistory("user1", null, null, 0, 20)).willReturn(RESPONSE);

        mockMvc.perform(get(URL).with(user("user1")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자가 다른 사람의 이력을 요청하면 거부하고 내부 서버를 호출하지 않는다")
    void otherUser_denied() throws Exception {
        mockMvc.perform(get(URL).param("username", "victim").with(user("user1")))
                .andExpect(status().is(new AdminAccessDeniedException().getStatus().value()));

        verifyNoInteractions(dailyReturnApiClient);
    }

    @Test
    @DisplayName("관리자는 username으로 다른 사용자의 이력을 조회한다")
    void adminViewsOtherUser() throws Exception {
        given(dailyReturnApiClient.getHistory("user1", null, null, 0, 20)).willReturn(RESPONSE);

        mockMvc.perform(get(URL).param("username", "user1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내부 서버 호출이 실패하면(null) 500을 반환한다")
    void clientFails_500() throws Exception {
        given(dailyReturnApiClient.getHistory("user1", null, null, 0, 20)).willReturn(null);

        mockMvc.perform(get(URL).with(user("user1")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("날짜 형식이 yyyy-MM-dd가 아니면 400을 반환한다")
    void invalidDate_400() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026/09/01").with(user("user1")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dailyReturnApiClient);
    }

    @Test
    @DisplayName("로그인하지 않으면 401, 인증 주체가 null이면 401을 반환한다")
    void unauthenticated_401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());

        assertThat(controller.getHistory(null, null, null, null, 0, 20).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(dailyReturnApiClient);
    }
}
