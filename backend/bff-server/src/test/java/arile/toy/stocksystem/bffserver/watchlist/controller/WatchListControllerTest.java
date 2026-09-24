package arile.toy.stocksystem.bffserver.watchlist.controller;

import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListNotFoundException;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import arile.toy.stocksystem.bffserver.watchlist.dto.AddWatchListRequest;
import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;
import arile.toy.stocksystem.bffserver.watchlist.service.WatchListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchListController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class WatchListControllerTest {

    private static final String URL = "/api/v1/watchlist";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private WatchListService watchListService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"GET, /api/v1/watchlist", "POST, /api/v1/watchlist", "DELETE, /api/v1/watchlist/005930"})
    @DisplayName("로그인하지 않으면 401이다")
    void unauthenticated_401(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockCode\": \"005930\", \"stockName\": \"삼성전자\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(watchListService);
    }

    @Test
    @DisplayName("조회: 로그인 사용자의 관심종목을 종목코드·종목명·순서로 반환한다")
    void getWatchList() throws Exception {
        given(watchListService.getAll("user1")).willReturn(List.of(
                WatchListEntity.of("user1", "005930", "삼성전자", 0),
                WatchListEntity.of("user1", "000660", "SK하이닉스", 1)));

        mockMvc.perform(get(URL).with(user("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"))
                .andExpect(jsonPath("$[0].stockName").value("삼성전자"))
                .andExpect(jsonPath("$[1].sortOrder").value(1));
    }

    @Test
    @DisplayName("추가: 로그인 사용자 기준으로 추가하고 저장된 항목을 반환한다")
    void add() throws Exception {
        given(watchListService.add("user1", "005930", "삼성전자"))
                .willReturn(WatchListEntity.of("user1", "005930", "삼성전자", 3));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockCode\": \"005930\", \"stockName\": \"삼성전자\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(3));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"stockName\": \"삼성전자\"}",
            "{\"stockCode\": \" \", \"stockName\": \"삼성전자\"}",
            "{\"stockCode\": \"005930\"}"
    })
    @DisplayName("추가: 종목코드·종목명이 비어 있으면 400이다")
    void add_invalid(String body) throws Exception {
        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(watchListService);
    }

    @Test
    @DisplayName("추가: 이미 있는 종목이면 409이고 Slack 알림을 보내지 않는다")
    void add_conflict() throws Exception {
        given(watchListService.add("user1", "005930", "삼성전자"))
                .willThrow(new WatchListAlreadyExistsException("005930"));

        mockMvc.perform(post(URL).with(user("user1")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockCode\": \"005930\", \"stockName\": \"삼성전자\"}"))
                .andExpect(status().isConflict());

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("삭제: 204를 반환하고, 없는 종목이면 404이다")
    void remove() throws Exception {
        willThrow(new WatchListNotFoundException("000660")).given(watchListService).remove("user1", "000660");

        mockMvc.perform(delete(URL + "/005930").with(user("user1"))).andExpect(status().isNoContent());
        mockMvc.perform(delete(URL + "/000660").with(user("user1"))).andExpect(status().isNotFound());

        verify(watchListService).remove("user1", "005930");
    }

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 세 API 모두 401을 반환한다")
    void nullPrincipal_401() {
        WatchListController controller = new WatchListController(watchListService);

        assertThat(controller.getWatchList(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.addToWatchList(null, new AddWatchListRequest("005930", "삼성전자")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.removeFromWatchList(null, "005930").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(watchListService);
    }
}
