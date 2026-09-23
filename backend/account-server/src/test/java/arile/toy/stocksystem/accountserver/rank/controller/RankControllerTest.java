package arile.toy.stocksystem.accountserver.rank.controller;

import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryItem;
import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryResponse;
import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.rank.service.RankHistoryQueryService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankController.class)
@ActiveProfiles("test")
class RankControllerTest {

    private static final String USERNAME = "user1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRankRepository userRankRepository;

    @MockitoBean
    private RankHistoryQueryService rankHistoryQueryService;

    @Test
    @DisplayName("GET /internal/ranks/{username}: 현재 랭크와 다음 등급까지의 RP 정보를 반환한다")
    void getRank() throws Exception {
        UserRankEntity rank = UserRankEntity.of(USERNAME, 1_000_000L);
        rank.setEntered(true);
        rank.setRp(3_800L);
        rank.setCurrentLevel(RankLevel.GOLD_4);
        rank.setHighestTierReached(RankLevel.PLATINUM_5);
        given(userRankRepository.findByUsername(USERNAME)).willReturn(Optional.of(rank));

        mockMvc.perform(get("/internal/ranks/{username}", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.tier").value("GOLD"))
                .andExpect(jsonPath("$.subTier").value(4))
                .andExpect(jsonPath("$.rp").value(3_800))
                .andExpect(jsonPath("$.highestTierReached").value("PLATINUM_5"))
                .andExpect(jsonPath("$.currentRankMinRp").value(3_750))
                .andExpect(jsonPath("$.nextRankMinRp").value(4_250));
    }

    @Test
    @DisplayName("GET /internal/ranks/{username}: 랭크가 없으면 예외가 처리되지 않고 전파된다 (500)")
    void getRank_notFound() {
        given(userRankRepository.findByUsername(USERNAME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> mockMvc.perform(get("/internal/ranks/{username}", USERNAME)))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Rank not found: " + USERNAME);
    }

    @Test
    @DisplayName("GET /internal/ranks/{username}/history: 페이지 파라미터가 없으면 0페이지, 20개로 조회한다")
    void getRankHistory_defaultParams() throws Exception {
        given(rankHistoryQueryService.getHistory(USERNAME, 0, 20))
                .willReturn(new RankHistoryResponse(List.of(), false));

        mockMvc.perform(get("/internal/ranks/{username}/history", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("GET /internal/ranks/{username}/history: 요청한 페이지의 이력을 반환한다")
    void getRankHistory() throws Exception {
        given(rankHistoryQueryService.getHistory(USERNAME, 1, 5))
                .willReturn(new RankHistoryResponse(List.of(
                        new RankHistoryItem(LocalDate.of(2026, 9, 24), "SILVER", 5, 1_800L, 150L)), true));

        mockMvc.perform(get("/internal/ranks/{username}/history", USERNAME)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].date").value("2026-09-24"))
                .andExpect(jsonPath("$.items[0].tier").value("SILVER"))
                .andExpect(jsonPath("$.items[0].subTier").value(5))
                .andExpect(jsonPath("$.items[0].rp").value(1_800))
                .andExpect(jsonPath("$.items[0].rpChange").value(150))
                .andExpect(jsonPath("$.hasNext").value(true));
    }
}
