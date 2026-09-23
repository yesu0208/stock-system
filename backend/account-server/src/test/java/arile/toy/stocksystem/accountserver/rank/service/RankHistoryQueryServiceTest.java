package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryItem;
import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryResponse;
import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import arile.toy.stocksystem.accountserver.rank.repository.RankHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RankHistoryQueryServiceTest {

    private static final String USERNAME = "user1";

    @Mock
    private RankHistoryRepository rankHistoryRepository;

    @InjectMocks
    private RankHistoryQueryService service;

    private static RankHistoryEntity history(LocalDate date, RankLevel level, long rp, long rpChange) {
        return RankHistoryEntity.of(USERNAME, date, level, rp, rpChange);
    }

    @Test
    @DisplayName("요청한 페이지의 이력을 최신순 그대로 응답 항목으로 변환하고, 다음 페이지 여부를 담는다")
    void getHistory_withNextPage() {
        PageRequest pageable = PageRequest.of(0, 2);
        List<RankHistoryEntity> content = List.of(
                history(LocalDate.of(2026, 9, 24), RankLevel.SILVER_5, 1_800L, 150L),
                history(LocalDate.of(2026, 9, 23), RankLevel.BRONZE_1, 1_650L, 0L));
        given(rankHistoryRepository.findByUsernameOrderByRecordDateDesc(USERNAME, pageable))
                .willReturn(new PageImpl<>(content, pageable, 5));

        RankHistoryResponse response = service.getHistory(USERNAME, 0, 2);

        assertThat(response.items()).containsExactly(
                new RankHistoryItem(LocalDate.of(2026, 9, 24), "SILVER", 5, 1_800L, 150L),
                new RankHistoryItem(LocalDate.of(2026, 9, 23), "BRONZE", 1, 1_650L, 0L));
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지면 hasNext는 false다")
    void getHistory_lastPage() {
        PageRequest pageable = PageRequest.of(2, 2);
        List<RankHistoryEntity> content = List.of(
                history(LocalDate.of(2026, 9, 20), RankLevel.BRONZE_5, 1_000L, 0L));
        given(rankHistoryRepository.findByUsernameOrderByRecordDateDesc(USERNAME, pageable))
                .willReturn(new PageImpl<>(content, pageable, 5));

        RankHistoryResponse response = service.getHistory(USERNAME, 2, 2);

        assertThat(response.items()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("이력이 없으면 빈 목록과 hasNext=false를 반환한다")
    void getHistory_empty() {
        PageRequest pageable = PageRequest.of(0, 20);
        given(rankHistoryRepository.findByUsernameOrderByRecordDateDesc(USERNAME, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        RankHistoryResponse response = service.getHistory(USERNAME, 0, 20);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }
}
