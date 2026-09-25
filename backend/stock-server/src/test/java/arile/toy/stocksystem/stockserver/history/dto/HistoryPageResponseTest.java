package arile.toy.stocksystem.stockserver.history.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 이력 페이지 응답 변환 테스트")
class HistoryPageResponseTest {

    @DisplayName("페이지 내용·번호·크기·전체 개수·다음 페이지 여부를 옮긴다")
    @Test
    void whenConverting_thenMapsPage() {
        var response = HistoryPageResponse.of(new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5));

        assertThat(response).isEqualTo(new HistoryPageResponse<>(List.of("a", "b"), 1, 2, 5, true));
        assertThat(HistoryPageResponse.of(new PageImpl<>(List.of("e"), PageRequest.of(2, 2), 5)).hasNext()).isFalse();
    }
}
