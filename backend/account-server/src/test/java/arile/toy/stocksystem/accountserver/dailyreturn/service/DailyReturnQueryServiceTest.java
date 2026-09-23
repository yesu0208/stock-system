package arile.toy.stocksystem.accountserver.dailyreturn.service;

import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryItem;
import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryResponse;
import arile.toy.stocksystem.accountserver.dailyreturn.entity.DailyReturnHistoryEntity;
import arile.toy.stocksystem.accountserver.dailyreturn.repository.DailyReturnHistoryRepository;
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
class DailyReturnQueryServiceTest {

    private static final String USERNAME = "user1";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);

    @Mock
    private DailyReturnHistoryRepository dailyReturnHistoryRepository;

    @InjectMocks
    private DailyReturnQueryService service;

    private static DailyReturnHistoryEntity entity() {
        return DailyReturnHistoryEntity.of(USERNAME, DATE, 1_000_000_000L, 1_050_000_000L,
                50_000_000L, 5.0, 30_000_000L, 50_000_000L, 5.0);
    }

    @Test
    @DisplayName("기간·페이지 조건으로 조회해 응답 항목으로 변환하고, 다음 페이지 여부를 담는다")
    void getHistory_withRange() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        PageRequest pageable = PageRequest.of(0, 1);
        given(dailyReturnHistoryRepository.search(USERNAME, from, to, pageable))
                .willReturn(new PageImpl<>(List.of(entity()), pageable, 3));

        DailyReturnHistoryResponse response = service.getHistory(USERNAME, from, to, 0, 1);

        assertThat(response.items()).containsExactly(new DailyReturnHistoryItem(
                DATE, 1_000_000_000L, 1_050_000_000L, 50_000_000L, 5.0, 30_000_000L, 50_000_000L, 5.0));
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("기간을 지정하지 않으면 null로 넘겨 전체 기간을 조회한다")
    void getHistory_withoutRange() {
        PageRequest pageable = PageRequest.of(0, 20);
        given(dailyReturnHistoryRepository.search(USERNAME, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        DailyReturnHistoryResponse response = service.getHistory(USERNAME, null, null, 0, 20);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }
}
