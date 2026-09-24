package arile.toy.stocksystem.bffserver.news.controller;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.news.dto.NaverNewsItem;
import arile.toy.stocksystem.bffserver.news.service.NewsService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private NewsService newsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("로그인 사용자는 검색어로 뉴스 목록을 조회한다")
    void getNews() throws Exception {
        given(newsService.searchNews("삼성전자")).willReturn(List.of(
                new NaverNewsItem("삼성전자 신고가", "o", "l", "d", "2026.09.24 09:05")));

        mockMvc.perform(get("/api/v1/news").param("keyword", "삼성전자").with(user("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("삼성전자 신고가"))
                .andExpect(jsonPath("$[0].pubDate").value("2026.09.24 09:05"));
    }

    @Test
    @DisplayName("로그인하지 않으면 401이다 (네이버 API 호출 한도 보호)")
    void unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/news").param("keyword", "삼성전자"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("네이버 API 장애면 503과 안내 문구로 응답하고 Slack 알림은 보내지 않는다")
    void naverUnavailable_503() throws Exception {
        given(newsService.searchNews("삼성전자")).willThrow(new ClientErrorException(
                HttpStatus.SERVICE_UNAVAILABLE, "뉴스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."));

        mockMvc.perform(get("/api/v1/news").param("keyword", "삼성전자").with(user("user1")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("뉴스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."));

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("검색어 파라미터가 없으면 400이고 Slack 알림을 보내지 않는다")
    void missingKeyword_400() throws Exception {
        mockMvc.perform(get("/api/v1/news").with(user("user1")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(newsService, slackNotifier);
    }
}
